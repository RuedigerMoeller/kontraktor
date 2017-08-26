package com.github.yuchi.semver;

public class SemVer {

	public static boolean isGreaterThenRange(Object version, Object range) {
		return isGreaterThenRange(version, range, false);
	}

	public static boolean isGreaterThenRange(
		Object version, Object range, boolean loose) {

		return isOutside(version, range, Direction.HIGH, loose);
	}

	public static boolean isOutside(
		Object version, Object range, Direction dir, boolean loose) {

		Version _version = Version.from(version, loose);
		Range _range = Range.from(range, loose);

		return _range.isOutside(_version, dir);
	}

	public static boolean isLessThenRange(Object version, Object range) {
		return isLessThenRange(version, range, false);
	}

	public static boolean isLessThenRange(
		Object version, Object range, boolean loose) {

		return isOutside(version, range, Direction.LOW, loose);
	}

	public static String valid(Object version) {
		return valid(version, false);
	}

	public static String valid(Object version, boolean loose) {
		Version _version = Version.from(version, loose);

		if (_version != null) {
			return _version.version;
		}
		else {
			return null;
		}
	}

	public static String validRange(Object range) {
		return validRange(range, false);
	}

	public static String validRange(Object range, boolean loose) {
		Range _range = Range.from(range, loose);

		if (_range != null) {
			return _range.raw;
		}
		else {
			return null;
		}
	}

}
