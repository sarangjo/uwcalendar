#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"

#include <iostream>
#include <queue>

using namespace cv;
using namespace std;

#define THRESHOLD			15
#define IN_THRESH(x,y)		abs((x) - (y)) < THRESHOLD
#define RANGE_THRESH		30

void drawLine(Mat m, Vec4i line, Scalar color) {
	cv::line(m, Point(line[0], line[1]), Point(line[2], line[3]), color, 3, LINE_AA);
}

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
	vector<vector<Point>> contours;
	findContours(edges, contours, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);

	// - Go through each of the contours and convert them into high and low Y values
	vector<Vec2i> ranges;
	for (vector<Point> contour : contours) {
		std::sort(contour.begin(), contour.end(), [](Point a, Point b) {
			return a.y < b.y;
		});
		Vec2i range = { contour.front().y, contour.back().y };
		ranges.push_back(range);
	}

	std::sort(ranges.begin(), ranges.end(), [](Vec2i a, Vec2i b) {
		if (a[0] == b[0]) {
			return a[1] < b[1];
		}
		return a[0] < b[0];
	});
	
	// - Attempt to normalize and combine each of the vertical ranges and eliminate non-timeline elements
	Mat rangesMat = Mat::zeros(edges.size(), CV_8UC3);
	vector<Vec2i> combinedRanges;
	Vec2i combinedRange = ranges[0];
	for (size_t i = 1; i < ranges.size(); i++) {
		Vec2i range = ranges[i];
		// If the range is already contained, no extension needed
		if (range[1] < combinedRange[1]) {
		}
		else if (range[0] < combinedRange[1]) {
			// the ranges overlap, but is it a stray random range?
			if (range[1] - combinedRange[1] < RANGE_THRESH) {
				// nah it's not random let's keep it
				combinedRange[1] = range[1];
			}
			else {
				// it's a stray range, ignore it
			}
		}
		else if (range[0] - combinedRange[1] < RANGE_THRESH) {
			// we extend even though it doesn't overlap
			combinedRange[1] = range[1];
		}
		else {
			// Move on in life
			combinedRanges.push_back(combinedRange);
			drawLine(rangesMat, Vec4i(50, combinedRange[0], 50, combinedRange[1]), Scalar(255, 255, 255));

			combinedRange = range;
		}
	}
	combinedRanges.push_back(combinedRange);
	drawLine(rangesMat, Vec4i(50, combinedRange[0], 50, combinedRange[1]), Scalar(255, 255, 255));

	imwrite("../data/timelineRanges.jpg", rangesMat);

	// - Finally, average out the distances between consecutive contours (top value to top value) and use as hour height
	
	// Ignore the zeroeth range because that's the one cut in half
	uint32_t hourHeightSum = 0;
	for (size_t i = 1; i < combinedRanges.size() - 1; i++) {
		hourHeightSum += (combinedRanges[i + 1] - combinedRanges[i])[0];
	}

	return hourHeightSum / (combinedRanges.size() - 2);
}

void processDay(Mat daySchedule) {
	// TODO: implement
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
	imwrite("../data/sched.jpg", sched);
	
	// Figure out how tall an hour is
	Mat timeline = edges(Range(leftEdge[3], leftEdge[1]), Range(0, topEdge[0]));
	uint32_t hourHeight = getHourHeight(timeline);
	cout << "Found hour height: " << hourHeight << endl;

	// Process the schedule day by day; for now assume all 5 days are in the schedule
	for (size_t i = 0; i < 5; i++) {
		Mat daySchedule = sched(Range(0, sched.rows), Range(i * sched.cols / 5, (i + 1) * (sched.cols / 5)));
		imwrite("../data/day" + to_string(i) + ".jpg", daySchedule);

		// TODO: process
		processDay(daySchedule);
	}

	imwrite("../data/output.jpg", output);
	waitKey();
	return 0;
}
