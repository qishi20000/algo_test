#include <cstdlib>
#include "list.h"

int data[] = { 1,2,3,4,5,6,7,8,9,10 };

int main() {
	List<int> a;
	for (auto i = 0; i < sizeof(data) / sizeof(data[0]); i++) {
		a.push_back(data[i]);
	}
	a.reverse();
}