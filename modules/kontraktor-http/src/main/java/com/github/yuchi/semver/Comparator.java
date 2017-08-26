package com.github.yuchi.semver;

import java.util.regex.Matcher;

public class Comparator {

	public static boolean compare(
		Object a, String op, Object b, boolean loose) {

		return compare(
			Version.from(a, loose), Operator.from(op), Version.from(b, loose));
	}

	public static boolean compare(Version a, Operator op, Version b) {
		switch (op) {
		case ANY:
			return true;
		case EQ:
			return a.compareTo(b) == 0;
		case NEQ:
			return a.compareTo(b) != 0;
		case GT:
			return a.compareTo(b) > 0;
		case GTE:
			return a.compareTo(b) >= 0;
		case LT:
			return a.compareTo(b) < 0;
		case LTE:
			return a.compareTo(b) <= 0;
		default:
			throw new IllegalArgumentException();
		}
	}

	public Comparator(String raw) throws IllegalArgumentException {
		this(raw, false);
	}

	public Comparator(String raw, boolean loose)
		throws IllegalArgumentException {

		this.raw = raw;
		this.loose = loose;

		REWrapper re = this.loose
			? Constants.COMPARATORLOOSE : Constants.COMPARATOR;

		Matcher m = re.match(raw);

		if (!m.matches()) {
			throw new IllegalArgumentException("Invalid comparator " + raw);
		}

		String op = m.group(1);
		String version = m.group(2);

		if ((version == null) || version.isEmpty()) {
			this.operator = Operator.ANY;
			this.version = null;
		}
		else {
			this.operator = Operator.from(op);
			this.version = new Version(version, this.loose);
		}
	}

	public boolean test(Object obj) {
		if (this.operator == Operator.ANY) {
			return true;
		}
		else {
			Version version = Version.from(obj, this.loose);

			return compare(version, this.operator, this.version);
		}
	}

	protected final boolean loose;
	protected final String raw;
	protected final Operator operator;
	protected final Version version;

}
