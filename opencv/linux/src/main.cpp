#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"

#include <iostream>
#include <limits.h>

#include "HourHeight.h"
#include "Tools.h"
#include "OCR.h"

using namespace cv;
using namespace std;

#define THRESHOLD			15
#define IN_THRESH(x,y)		abs((x) - (y)) < THRESHOLD

#define SCHEDULE_FILE	"schedule.jpg"

string folder;
uint32_t hourHeight;

// TODO
Vec4i findLeftEdge(vector<Vec4i> lines, size_t width) {
    vector<Vec4i> desiredLines = vector<Vec4i>();

    // Only verticals on the left quarter of the image
    for (auto line : lines) {
        if (IN_THRESH(line[0], line[2]) && (line[0] < width / 4) && (line[2] < width / 4)) {
            desiredLines.push_back(line);
        }
    }

    Vec4i rightLine = { -1, -1, -1, -1 };

    // Now we want the farthest right but also the longest one
    if (desiredLines.size() > 0) {
        rightLine = desiredLines[0];

        // pick the farthest right
        for (auto l : desiredLines) {
            if (l[0] > rightLine[0]) {
                rightLine = l;
            }
        }

        // choose the longest
        for (auto l : desiredLines) {
            if (IN_THRESH(l[0], rightLine[0])) {
                if ((abs(l[3] - l[1]) > abs(rightLine[3] - rightLine[1]))) {
                    rightLine = l;
                }
            }
        }
    }
    return rightLine;
}

// TODO
Vec4i findTopEdge(vector<Vec4i> lines, Vec4i leftEdge) {
    Vec4i topEdge = { -1, -1, -1, -1 };

    // Find the longest line that shares a point with the left edge
    for (size_t i = 0; i < lines.size(); i++) {
        Vec4i l = lines[i];
        // Horizontal?
        if (IN_THRESH(l[1], l[3])) {
            // Shares point?
            if (IN_THRESH(l[0], leftEdge[2]) && IN_THRESH(l[1], leftEdge[3])) {
                // Is longest?
                if (topEdge[0] >= 0) {
                    if (abs(topEdge[2] - topEdge[0]) < abs(l[2] - l[0])) {
                        topEdge = l;
                    }
                }
                else {
                    topEdge = l;
                }
            }
        }
    }

    return topEdge;
}

// TODO
Mat processDay(Mat daySchedule, uint32_t dayIndex, uint32_t hourHeight) {
    cout << "Processing day " << dayIndex << endl;

    Mat dsGray = daySchedule;
    cvtColor(daySchedule, dsGray, CV_BGR2GRAY);

    // Threshold and cancel out inconsistencies in the left and right edges
    Mat thresh;
    threshold(dsGray, thresh, 240, 255, THRESH_BINARY_INV);
    rectangle(thresh, Point(0, 0), Point(thresh.cols / 10, thresh.rows), Scalar(0, 0, 0), CV_FILLED);
    rectangle(thresh, Point((int)(thresh.cols * 0.9), 0), Point(thresh.cols, thresh.rows), Scalar(0, 0, 0), CV_FILLED);

    // Find contours in the day's schedule
    vector<vector<Point>> contours;
    vector<Vec4i> hierarchy;
    cv::findContours(thresh, contours, hierarchy, CV_RETR_EXTERNAL, CHAIN_APPROX_NONE);
    vector<Vec2i> ranges = contoursToRanges(contours);
    vector<Vec2i> classRanges;

    // contoursMat contains our matrix with all the countours drawn
    Mat contoursMat = Mat::zeros(thresh.size(), CV_8UC3);
    for (size_t i = 0; i < contours.size(); i++)
    {
        Vec2i range = ranges[i];

        // If range is big enough, draw it
        if (range[1] - range[0] > hourHeight / 6) {
            classRanges.push_back(range);

            Scalar color = Scalar(0, 255, 0);
            drawContours(contoursMat, contours, i, color, 10, 8, hierarchy, 0);
        }
    }

    vector<Mat> classMats;
    // Extract the classes from daySchedule
    for (size_t i = 0; i < classRanges.size(); i++) {
        Vec2i range = classRanges[i];

        Mat classMat = daySchedule(Range(range[0], range[1]), Range(0, daySchedule.cols));
        // Mat classMatGray = classMat;
        // cvtColor(classMat, classMatGray, CV_BGR2GRAY);
        imwrite(folder + "/" + to_string(dayIndex) + "-" + to_string(i) + ".jpg", classMat);

        classMats.push_back(classMat);
    }

    // Convert to text
    //convertToText(classMats, dayIndex);

    return contoursMat;
}

int main(int argc, char** argv) {
    if (argc != 2) {
        cout << "Need folder" << endl;
        exit(1);
    }
    folder = argv[1];
    string filename = folder + "/" SCHEDULE_FILE;
    Mat src = imread(filename);
    if (src.empty()) {
        cout << "can't open " << filename << endl;
        return -1;
    }

    // Convert to gray
    Mat gray = src;
    cvtColor(src, gray, COLOR_BGR2GRAY);

    // Detect edges
    Mat edges;
    Canny(gray, edges, 50, 200);

    // Set up "output" matrix
    Mat output;
    // We want it BGR so that we can draw colored lines on it
    cvtColor(edges, output, COLOR_GRAY2BGR);

    // Detect lines from edges
    vector<Vec4i> lines;
    HoughLinesP(edges, lines, 1, CV_PI / 180, 50, 150, 10);

    // Find the left edge of our schedule area
    Vec4i leftEdge = findLeftEdge(lines, edges.cols);
    if (leftEdge[0] < 0) {
        return -1;
    }
    drawLine(output, leftEdge, Scalar(0, 0, 255));
    cout << "Found left edge: " << leftEdge << endl;

    // Find the top edge of our schedule area
    Vec4i topEdge = findTopEdge(lines, leftEdge);
    if (topEdge[0] < 0) {
        return -1;
    }
    drawLine(output, topEdge, Scalar(255, 0, 0));
    cout << "Found top edge: " << topEdge << endl;

    // Cut out the part of the schedule we care about
    Mat sched = src(Range(leftEdge[3], leftEdge[1]), Range(topEdge[0], topEdge[2]));

    // Figure out how tall an hour is
    Mat timeline = edges(Range(leftEdge[3], leftEdge[1]), Range(0, topEdge[0]));
    uint32_t hourHeight = getHourHeight(timeline);
    cout << "Found hour height: " << hourHeight << endl;

    vector<Mat> matrices;
    // Process the schedule day by day; for now assume all 5 days are in the schedule
    for (size_t i = 0; i < 5; i++) {
        Mat daySchedule = sched(Range(0, sched.rows), Range(i * sched.cols / 5, (i + 1) * (sched.cols / 5)));

        // Process each day and push the processed image into a list
        matrices.push_back(processDay(daySchedule, i, hourHeight));
        //break;
    }

    // Combine processed days into a single matrix horizontally
    Mat contourMat;
    hconcat(matrices, contourMat);
    imwrite(folder + "/output.jpg", contourMat);

    cout << "Done!" << endl;
    waitKey();

    exit(0);
    return 0;
}
