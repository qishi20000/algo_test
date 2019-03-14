#include <iostream>
#include <utility>
#include <random>
#include <chrono>

const int DATA_SIZE = 100000;

void bubblesort(int *data, unsigned int dataSize) {
	if (data == nullptr || dataSize == 0)
	{
		return;
	}
	unsigned int noChangeCount = 0;
	while (noChangeCount != dataSize - 1) {
		noChangeCount = 0;
		for (auto i = 0; i < dataSize - 1; i++) {
			if (data[i] <= data[i + 1]) {
				noChangeCount++;
				continue;
			}
			std::swap(data[i], data[i + 1]);
		}
	}

}

void insertSort(int *data, unsigned int dataSize) {
	if (data == nullptr || dataSize == 0)
	{
		return;
	}

	for (auto i = 1;i<dataSize;i++)
	{
		for (auto j = 0; j < i; j++) {
			if (data[j]>data[i])
			{
				int datai = data[i];
				int m = i;
				while (m>j)
				{
					data[m] = data[m - 1];
					m--;
				}
				data[j] = datai;
				break;
			}
		}

	}
}

//merge_sort
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

void merge_sort(int* data, unsigned int dataSize) {
	merge_sort_c(data, 0, dataSize - 1);
}


int main() {
	int* data = new int[DATA_SIZE];
	std::default_random_engine e;
	std::uniform_int_distribution<unsigned> u(0, 100000);
	for (auto i = 0; i < DATA_SIZE; i++)
	{
		data[i] = u(e);
	}
	auto start = std::chrono::system_clock::now();
	//bubblesort(data, DATA_SIZE);
	insertSort(data, DATA_SIZE);
	//merge_sort(data, DATA_SIZE);
	auto end = std::chrono::system_clock::now();
	//for (auto i = 0; i < DATA_SIZE; i++) {
	//	std::cout << "data[" << i << "]:" << data[i] << std::endl;
	//}
	
	auto durtime = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
	std::cout << "durtime: " << (double)durtime.count() <<"ms"<< std::endl;
	delete[] data;
	
}