#include <iostream>
#include <utility>
int data[] = { 5,7,3,0,6,8,9 };

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


int main() {
	//bubblesort(data, 7);
	insertSort(data, 7);
}