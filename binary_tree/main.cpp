#include <iostream>
#include <memory>
#include <vector>
#include <string>

template <class T>
struct binaryTreeNode {
	T data;
	std::shared_ptr<binaryTreeNode> leftNode = nullptr;
	std::shared_ptr<binaryTreeNode> rightNode = nullptr;
};

template <class T>
class binaryTree {
public:
	void push_data(const T& data) {

		if (isEmpty())
		{
			rootNode_ = std::make_shared<binaryTreeNode<T> >();
			rootNode_->data = data;
			nodeSequence_.push_back(rootNode_);
			currentPos_ = 0;
			return;
		}

		if (currentPos_ >= nodeSequence_.size())
		{
			std::cout << "the current pos:" << currentPos_ << " is error" << std::endl;
			return;
		}
		std::shared_ptr<binaryTreeNode<T> > emptyNode(nodeSequence_[currentPos_].lock());
		if (!emptyNode)
		{
			std::cout << "the current pos is nullptr" << std::endl;
			return;
		}

		if (!emptyNode->leftNode) {
			emptyNode->leftNode = std::make_shared<binaryTreeNode<T> >();
			emptyNode->leftNode->data = data;
			nodeSequence_.push_back(emptyNode->leftNode);
		}
		else if (!emptyNode->rightNode) {
			emptyNode->rightNode = std::make_shared<binaryTreeNode<T> >();
			emptyNode->rightNode->data = data;
			nodeSequence_.push_back(emptyNode->rightNode);
			currentPos_++;
		}
		else {
			std::cout << "the current pos:" << currentPos_ << " is not empty" << std::endl;
			return;
		}
	}
	bool isEmpty() const {
		return rootNode_ == nullptr?true:false;
	}



	void printFore() {
		printFore(rootNode_);
	}

	void printMid() {
		printMid(rootNode_);
	}

	void printBack() {
		printBack(rootNode_);
	}

private:
	void printFore(const std::shared_ptr<binaryTreeNode<T> >& node) {
		if (node == nullptr)
		{
			return;
		}
		std::cout <<" "<< node->data;
		printFore(node->leftNode);
		printFore(node->rightNode);
	}

	void printMid(const std::shared_ptr<binaryTreeNode<T> >& node) {
		if (node == nullptr)
		{
			return;
		}
		printMid(node->leftNode);
		std::cout << " " << node->data;
		printMid(node->rightNode);
	}

	void printBack(const std::shared_ptr<binaryTreeNode<T> >& node) {
		if (node == nullptr)
		{
			return;
		}
		printBack(node->leftNode);
		printBack(node->rightNode);
		std::cout << " " << node->data;
	}

private:
	std::shared_ptr<binaryTreeNode<T> > rootNode_ = nullptr;
	std::vector<std::weak_ptr<binaryTreeNode<T> > > nodeSequence_;
	unsigned int currentPos_ = 0;
};

int main() {
	binaryTree<std::string> treeA;
	for (auto i = 0x41;i<=0x5A;i++)
	{
		std::string data(1,i);
		treeA.push_data(data);
	}
	treeA.printFore();
	std::cout<<std::endl;
	treeA.printMid();
	std::cout << std::endl;
	treeA.printBack();
}