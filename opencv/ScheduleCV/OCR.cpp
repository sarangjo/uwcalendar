#include "OCR.h"

#include <fstream>

using namespace std;

void convertToText(vector<Mat> classMats, uint32_t dayIndex)
{
	ofstream classFile;
	classFile.open(to_string(dayIndex) + ".txt");
	classFile << "Day " << dayIndex << endl;
	for (auto classMat : classMats) {
		// Convert Mat to text
	}
}
