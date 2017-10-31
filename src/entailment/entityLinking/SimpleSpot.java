package entailment.entityLinking;

public class SimpleSpot implements Comparable<SimpleSpot> {
	public String spot;
	public int count;

	public SimpleSpot(String spot, int count) {
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
