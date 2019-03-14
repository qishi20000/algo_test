#include <iostream>

void merge(int* data, unsigned int startIndex, unsigned int middleIndex, unsigned int endIndex) {
	if (data == nullptr) {
		return;
	}

	if (endIndex - startIndex == 1) {
		if (data[startIndex] > data[endIndex]) {
			std::swap(data[startIndex], data[endIndex]);
		}
		return;
	}
	std::unique_ptr<int[]> dataTemp(new int[endIndex - startIndex + 1]);
	memcpy(dataTemp.get(), data + startIndex, sizeof(int)*(endIndex - startIndex + 1));

	unsigned int firstIndex = 0;
	unsigned int secondIndex = middleIndex - startIndex + 1;
	unsigned int dataIndex = startIndex;
	while ((firstIndex <= middleIndex - startIndex) && (secondIndex <= endIndex - startIndex))
	{
		if (dataTemp[firstIndex] <= dataTemp[secondIndex])
		{
			data[dataIndex] = dataTemp[firstIndex];
			firstIndex++;
		}
		else {
			data[dataIndex] = dataTemp[secondIndex];
			secondIndex++;
		}
		dataIndex++;
	}

	while (firstIndex <= middleIndex - startIndex) {
		data[dataIndex] = dataTemp[firstIndex];
		dataIndex++;
		firstIndex++;
	}

	while (secondIndex <= endIndex - startIndex) {
		data[dataIndex] = dataTemp[secondIndex];
		dataIndex++;
		secondIndex++;
	}

}

void merge_sort_c(int* data, unsigned int startIndex, unsigned int endIndex) {
	if (data == nullptr)
	{
		return;
	}

	if (startIndex >= endIndex)
	{
		return;
	}

	unsigned int middleIndex = (startIndex + endIndex) / 2;
	merge_sort_c(data, startIndex, middleIndex);
	merge_sort_c(data, middleIndex + 1, endIndex);
	merge(data, startIndex, middleIndex, endIndex);
}

void merge_sort(int* data,unsigned int dataSize) {
	merge_sort_c(data, 0, dataSize - 1);
}

int data[] = { 4,7,3,1,5,9 };

int main() {
	merge_sort(data, 6);
}