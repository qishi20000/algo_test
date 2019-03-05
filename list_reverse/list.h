#pragma once

#include <utility>

template <class T>
struct DataNode{
	T data;
	DataNode<T>* next = nullptr;
};

template <class T>
class List {
public:
	bool insert(unsigned int location,const T& data) {
		if (location >= size_) {
			std::cout << "location larger than size:" << size_;
			return false;
		}
		int i = 0;
		DataNode<T>* p = &headNode_;
		while (i < location) {
			p = headNode_.next;
			i++;
		}

		DataNode<T>* newNode = new DataNode<T>();
		newNode->next = p->next;
		p->next = newNode;
		size_++;
		return true;
	}

	bool push_back(const T& data) {
		DataNode<T>* newNode = nullptr;
		try {
			newNode = new DataNode<T>();
		}
		catch (...) {
			return false;
		}
		newNode->data = data;

		if (size_ == 0) {
			headNode_ = newNode;
			tailNode_ = newNode;
			size_++;
			return true;
		}

		tailNode_->next = newNode;
		tailNode_ = newNode;
		size_++;
		return true;
	}

	void clear() {
		while (size_ > 0) {
			DataNode<T>* headNextNode = headNode_->next;
			delete headNode_;
			headNode_ = headNextNode;
			size_--;
		}
		headNode_ = nullptr;
		tailNode_ = nullptr;
	}

	void reverse() {
		DataNode<T>* Nodeindex = headNode_;
		DataNode<T>* nextnextNode = Nodeindex->next->next;
		while (Nodeindex->next != nullptr) {
			DataNode<T>* nextNode = nextnextNode->next;
			nextnextNode = nextNode->next;
			nextNode->next = Nodeindex;
			Nodeindex = nextNode;
		}
		std::swap(headNode_, tailNode_);
		tailNode_->next = nullptr;
	}
	
private:
	DataNode<T>* headNode_ = nullptr;
	DataNode<T>* tailNode_ = nullptr;
	unsigned int size_ = 0;

};
