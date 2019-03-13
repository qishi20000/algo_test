#include <iostream>

class A {
public:
	A() {
		std::cout << "D" << std::endl;
	}
	A(const char* p) {
		std::cout << "A" << std::endl;
	}

	A(const A& a) {
		std::cout << "B" << std::endl;
	}

	A& operator = (const A& a) {
		std::cout << "C" << std::endl;
		return *this;
	}
};


int main() {
	A a = "abcd";
	A b = a;
	A c(b);

	char character[] = "abcd";
	bool data_true = true;
	float data_float = 3.4;
	double data_double = 3.12;
	int data_int = 4;
	long data_long = 9;
	short data_short = 19;
	int* ptr_int = &data_int;
	std::cout << "data_charactor: " << sizeof(character) << std::endl;
	std::cout << "data_true: " << sizeof(data_true) << std::endl;
	std::cout << "data_int: " << sizeof(data_int) << std::endl;
	std::cout << "data_long: " << sizeof(data_long) << std::endl;
	std::cout << "data_short: " << sizeof(data_short) << std::endl;
	std::cout << "data_float: " << sizeof(data_float) << std::endl;
	std::cout << "data_double: " << sizeof(data_double) << std::endl;
	std::cout << "ptr_int: " << sizeof(ptr_int) << std::endl;
}