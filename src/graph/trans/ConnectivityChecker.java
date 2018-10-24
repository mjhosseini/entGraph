package graph.trans;

public interface ConnectivityChecker {
	int isConnected(int i, int j);// -1: don't know, 0: no, 1: yes
}
