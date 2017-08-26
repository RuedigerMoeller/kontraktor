package com.github.yuchi.semver;

public enum Operator {
	ANY, EQ, NEQ, GT, GTE, LT, LTE;

	public static Operator from(String from) {
		if ((from == null) ||
			from.isEmpty() ||
			from.equals("=") ||
			from.equals("==")) {

			return EQ;
		}
		if (from.equals("!=")) {
			return NEQ;
		}
		else if (from.equals("<")) {
			return LT;
		}
		else if (from.equals("<=")) {
			return LTE;
		}
		else if (from.equals(">")) {
			return GT;
		}
		else if (from.equals(">=")) {
			return GTE;
		}
		else {
			return null;
		}
	}
}