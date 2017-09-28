#include "opencv2/imgproc/imgproc.hpp"

#include "Tools.h"

using namespace cv;

void drawLine(Mat m, Vec4i line, Scalar color) {
	cv::line(m, Point(line[0], line[1]), Point(line[2], line[3]), color, 3, LINE_AA);
}