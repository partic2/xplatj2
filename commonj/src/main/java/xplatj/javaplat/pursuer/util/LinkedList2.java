package xplatj.javaplat.pursuer.util;

public class LinkedList2 <T> {
	public static class Node<T> extends Container<T>{
		public Node<T> prev;
		public Node<T> next;
		public void remove() {
			prev.next=next;
			next.prev=prev;
			prev=null;
			next=null;
		}
		public void insertBefore(Node<T> node) {
			node.prev=prev;
			node.next=this;
			prev.next=node;
			prev=node;
		}
		public void insertAfter(Node<T> node) {
			next.prev=node;
			node.prev=this;
			node.next=next;
		}
	}
	protected Node<T> head;
}
