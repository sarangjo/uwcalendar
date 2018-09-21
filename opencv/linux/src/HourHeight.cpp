#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"

#include <fstream>
#include <functional>

#include "HourHeight.h"
#include "Tools.h"

using namespace cv;
using namespace std;

#define RANGE_THRESH		30

typedef vector<Point> Contour;

// Converts countours into 2d ranges
vector<Vec2i> contoursToRanges(vector<Contour> contours) {
	vector<Vec2i> ranges;
	for (Contour contour : contours) {
		std::sort(contour.begin(), contour.end(), [](Point a, Point b) {
			return a.y < b.y;
		});
		Vec2i range = { contour.front().y, contour.back().y };
		ranges.push_back(range);
	}
	return ranges;
}

uint32_t getHourHeight(Mat timeline) {
	Mat edges;
	Canny(timeline, edges, 50, 200);
	vector<Contour> contours;
	findContours(edges, contours, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);

	// - Go through each of the contours and convert them into high and low Y values
	vector<Vec2i> ranges = contoursToRanges(contours);

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

	// - Finally, average out the distances between consecutive contours (top value to top value) and use as hour height

	// Ignore the zeroeth range because that's the one cut in half
	uint32_t hourHeightSum = 0;
	for (size_t i = 1; i < combinedRanges.size() - 1; i++) {
		hourHeightSum += (combinedRanges[i + 1] - combinedRanges[i])[0];
	}

	return hourHeightSum / (combinedRanges.size() - 2);
}
