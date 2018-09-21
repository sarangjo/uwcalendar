#pragma once

#include "opencv2/imgproc/imgproc.hpp"

std::vector<cv::Vec2i> contoursToRanges(std::vector<std::vector<cv::Point>> contours);
std::uint32_t getHourHeight(cv::Mat timeline);
