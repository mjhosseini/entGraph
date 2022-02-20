package entailment.linkingTyping;

public class SimpleSpot implements Comparable<SimpleSpot> {
	public String spot;
	public double count;

	public SimpleSpot(String spot, double count) {
		this.spot = spot;
		this.count = count;
	}

	@Override
	public int compareTo(SimpleSpot ss) {
		if (count > ss.count) {
			return 1;
		} else if (count < ss.count) {
			return -1;
		}
		return 0;
	}
}
