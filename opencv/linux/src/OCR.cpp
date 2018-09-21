#include "OCR.h"

#include <fstream>
#include <iostream>

using namespace std;

void convertToText(vector<Mat> classMats, uint32_t dayIndex)
{
	cout << "Converting to text!" << endl;
	ofstream classFile;
	classFile.open(to_string(dayIndex) + ".txt");
	classFile << "Day " << dayIndex << endl;
	for (auto classMat : classMats) {
		// Convert Mat to text
	}
}
