#include "opencv2/imgcodecs.hpp"
#include "opencv2/highgui/highgui.hpp"
#include "opencv2/imgproc/imgproc.hpp"

#include <iostream>

using namespace cv;
using namespace std;

static void help()
{
	cout << "\nThis program demonstrates line finding with the Hough transform.\n"
		"Usage:\n"
		"./houghlines <image_name>, Default is ../data/pic1.png\n" << endl;
}

int main(int argc, char** argv)
{
	cv::CommandLineParser parser(argc, argv,
		"{help h||}{@image|../data/pic1.png|}"
	);
	if (parser.has("help"))
	{
		help();
		return 0;
	}
	string filename = parser.get<string>("@image");
	if (filename.empty())
	{
		help();
		cout << "no image_name provided" << endl;
		return -1;
	}
	Mat src = imread(filename, 0);
	if (src.empty())
	{
		help();
		cout << "can not open " << filename << endl;
		return -1;
	}

	// Find edges and convert to 
	Mat dst, cdst;
	Canny(src, dst, 50, 200, 3);
	cvtColor(dst, cdst, COLOR_GRAY2BGR);

#if 0
	vector<Vec2f> lines;
	HoughLines(dst, lines, 1, CV_PI / 180, 100, 0, 0);

	for (size_t i = 0; i < lines.size(); i++)
	{
		float rho = lines[i][0], theta = lines[i][1];
		Point pt1, pt2;
		double a = cos(theta), b = sin(theta);
		double x0 = a*rho, y0 = b*rho;
		pt1.x = cvRound(x0 + 1000 * (-b));
		pt1.y = cvRound(y0 + 1000 * (a));
		pt2.x = cvRound(x0 - 1000 * (-b));
		pt2.y = cvRound(y0 - 1000 * (a));
		line(cdst, pt1, pt2, Scalar(0, 0, 255), 3, CV_AA);
	}
#else
	vector<Vec4i> lines;
	HoughLinesP(dst, lines, 1, CV_PI / 180, 50, 150, 10);

	// 1. Find the left border of the 
	vector<Vec4i> desired_lines = vector<Vec4i>();

	int threshold = 15;

	for (size_t i = 0; i < lines.size(); i++)
	{
		Vec4i line = lines[i];
		// Only verticals on the left quarter of the image
		if (abs(line[0] - line[2]) < threshold && line[0] < dst.cols/4 && line[2] < dst.cols/4) {
			desired_lines.push_back(line);
		}
	}
#endif
	//imshow("source", src);
	//imshow("detected lines", cdst);

	// Now we want the farthest right but also the longest one

	if (desired_lines.size() > 0) {
		Vec4i rightLine = desired_lines[0];

		// pick the farthest right of the two
		for (size_t i = 1; i < desired_lines.size(); i++) {
			Vec4i l = desired_lines[i];
			if (l[0] > rightLine[0]) {
				rightLine = l;
			}
		}

		// choose the longer of the two
		for (size_t i = 0; i < desired_lines.size(); i++) {
			Vec4i l = desired_lines[i];
			if (abs(l[0] - rightLine[0]) < threshold) {
				if (abs(l[3] - l[1]) > abs(rightLine[3] - rightLine[1])) {
					rightLine = l;
				}
			}

		}

		line(cdst, Point(rightLine[0], rightLine[1]), Point(rightLine[2], rightLine[3]), Scalar(0, 0, 255), 3, LINE_AA);

		imwrite("../data/output.jpg", cdst);
	}


	waitKey();

	return 0;
}
