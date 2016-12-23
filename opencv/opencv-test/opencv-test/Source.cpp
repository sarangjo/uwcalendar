#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"

#include <iostream>

using namespace cv;
using namespace std;

#define THRESHOLD			15
#define IN_THRESH(x,y)		abs((x) - (y)) < THRESHOLD

Vec4i findLeftEdge(vector<Vec4i> lines, size_t width) {
	vector<Vec4i> desiredLines = vector<Vec4i>();

	// Only verticals on the left quarter of the image
	for (auto line : lines)
	{
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

uint32_t getHourHeight(Mat timeline) {
	Mat edges;
	Canny(timeline, edges, 50, 200);

	// Go through the edges somehow
	vector<vector<Point>> contours;
	vector<Point> heirarchy;
	findContours(edges, contours, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);

	Mat timelineContours = Mat::zeros(edges.size(), CV_8UC3);
	for (int i = 0; i < contours.size(); i++) {
		drawContours(timelineContours, contours, i, Scalar(255, 255, 255));
	}
	imwrite("../data/timelineContours.jpg", timelineContours);

	// TODO:
	// - Go through each of the contours and convert them into high and low Y values
	// - Attempt to normalize each of the vertical ranges and eliminate non-timeline elements
	// - Finally, average out the distances between consecutive contours (top value to top value) and use as hour height

	return 0;
}

int main(int argc, char** argv)
{
	string filename = "../data/schedule.jpg";
	Mat src = imread(filename);
	if (src.empty())
	{
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
	line(output, Point(leftEdge[0], leftEdge[1]), Point(leftEdge[2], leftEdge[3]), Scalar(0, 0, 255), 10, LINE_AA);
	cout << "Found left edge: " << leftEdge << endl;

	// Find the top edge of our schedule area
	Vec4i topEdge = findTopEdge(lines, leftEdge);
	if (topEdge[0] < 0) {
		return -1;
	}
	line(output, Point(topEdge[0], topEdge[1]), Point(topEdge[2], topEdge[3]), Scalar(255, 0, 0), 3, LINE_AA);
	cout << "Found top edge: " << topEdge << endl;

	// Cut out the part of the schedule we care about
	Mat sched = src(Range(leftEdge[3], leftEdge[1]), Range(topEdge[0], topEdge[2]));
	imwrite("../data/sched.jpg", sched);
	
	// Now figure out how tall an hour is
	Mat timeline = edges(Range(leftEdge[3], leftEdge[1]), Range(0, topEdge[0]));
	imwrite("../data/timeline.jpg", timeline);
	uint32_t hourHeight = getHourHeight(timeline);

	imwrite("../data/output.jpg", output);
	waitKey();
	return 0;
}
