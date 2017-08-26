package com.github.yuchi.semver;

public class Constants {

	protected static REWrapper wrap(String pattern) {
		return new REWrapper(pattern);
	}

	protected static REWrapper wrap(String pattern, boolean global) {
		return new REWrapper(pattern, global);
	}

	public static final REWrapper NUMERIC = wrap("^[0-9]+$");

	public static final REWrapper NUMERICIDENTIFIER = wrap("0|[1-9]\\d*");
	public static final REWrapper NUMERICIDENTIFIERLOOSE = wrap("[0-9]+");


	// ## Non-numeric Identifier
	// Zero or more digits, followed by a letter or hyphen, and then zero or
	// more letters, digits, or hyphens.

	public static final REWrapper NONNUMERICIDENTIFIER = wrap(
		"\\d*[a-zA-Z-][a-zA-Z0-9-]*");


	// ## Main Version
	// Three dot-separated numeric identifiers.

	public static final REWrapper MAINVERSION = wrap(
			"(" + NUMERICIDENTIFIER + ")\\." +
			"(" + NUMERICIDENTIFIER + ")\\." +
			"(" + NUMERICIDENTIFIER + ")");

	public static final REWrapper MAINVERSIONLOOSE = wrap(
			"(" + NUMERICIDENTIFIERLOOSE + ")\\." +
			"(" + NUMERICIDENTIFIERLOOSE + ")\\." +
			"(" + NUMERICIDENTIFIERLOOSE + ")");

	// ## Pre-release Version Identifier
	// A numeric identifier, or a non-numeric identifier.

	public static final REWrapper PRERELEASEIDENTIFIER = wrap(
			"(?:" + NUMERICIDENTIFIER +
			"|" + NONNUMERICIDENTIFIER + ")");

	public static final REWrapper PRERELEASEIDENTIFIERLOOSE = wrap(
			"(?:" + NUMERICIDENTIFIERLOOSE +
			"|" + NONNUMERICIDENTIFIER + ")");

	// ## Pre-release Version
	// Hyphen, followed by one or more dot-separated pre-release version
	// identifiers.

	public static final REWrapper PRERELEASE = wrap(
			"(?:-(" + PRERELEASEIDENTIFIER +
			"(?:\\." + PRERELEASEIDENTIFIER + ")*))");

	public static final REWrapper PRERELEASELOOSE = wrap(
			"(?:-?(" + PRERELEASEIDENTIFIERLOOSE +
			"(?:\\." + PRERELEASEIDENTIFIERLOOSE + ")*))");

	// ## Build Metadata Identifier
	// Any combination of digits, letters, or hyphens.

	public static final REWrapper BUILDIDENTIFIER = wrap("[0-9A-Za-z-]+");

	// ## Build Metadata
	// Plus sign, followed by one or more period-separated build metadata
	// identifiers.

	public static final REWrapper BUILD = wrap(
			"(?:\\+(" + BUILDIDENTIFIER +
			"(?:\\." + BUILDIDENTIFIER + ")*))");

	// ## Full Version String
	// A main version, followed optionally by a pre-release version and
	// build metadata.

	// Note that the only major, minor, patch, and pre-release sections of
	// the version string are capturing groups.  The build metadata is not a
	// capturing group, because it should not ever be used in version
	// comparison.

	public static final REWrapper FULLPLAIN = wrap(
			"v?" + MAINVERSION + PRERELEASE + "?" + BUILD + "?");

	public static final REWrapper FULL = wrap("^" + FULLPLAIN + "$");

	// like full, but allows v1.2.3 and =1.2.3, which people do sometimes.
	// also, 1.0.0alpha1 (prerelease without the hyphen) which is pretty
	// common in the npm registry.

	public static final REWrapper LOOSEPLAIN = wrap(
			"[v=\\s]*" + MAINVERSIONLOOSE +
			PRERELEASELOOSE + "?" +
			BUILD + "?");

	public static final REWrapper LOOSE = wrap("^" + LOOSEPLAIN + "$");

	public static final REWrapper GTLT = wrap("((?:<|>)?=?)");

	// Something like "2.*" or "1.2.x".
	// Note that "x.x" is a valid xRange identifer, meaning "any version"
	// Only the first item is strictly required.

	public static final REWrapper XRANGEIDENTIFIERLOOSE = wrap(
			NUMERICIDENTIFIERLOOSE + "|x|X|\\*");

	public static final REWrapper XRANGEIDENTIFIER = wrap(
			NUMERICIDENTIFIER + "|x|X|\\*");

	public static final REWrapper XRANGEPLAIN = wrap(
			"[v=\\s]*(" + XRANGEIDENTIFIER + ")" +
			"(?:\\.(" + XRANGEIDENTIFIER + ")" +
			"(?:\\.(" + XRANGEIDENTIFIER + ")" +
			"(?:" + PRERELEASE + ")?" +
			BUILD + "?" +
			")?)?");

	public static final REWrapper XRANGEPLAINLOOSE = wrap(
			"[v=\\s]*(" + XRANGEIDENTIFIERLOOSE + ")" +
			"(?:\\.(" + XRANGEIDENTIFIERLOOSE + ")" +
			"(?:\\.(" + XRANGEIDENTIFIERLOOSE + ")" +
			"(?:" + PRERELEASELOOSE + ")?" +
			BUILD + "?" +
			")?)?");

	public static final REWrapper XRANGE = wrap(
			"^" + GTLT + "\\s*" + XRANGEPLAIN + "$");
	public static final REWrapper XRANGELOOSE = wrap(
			"^" + GTLT + "\\s*" + XRANGEPLAINLOOSE + "$");

	// Tilde ranges.
	// Meaning is "reasonably at or greater than"

	public static final REWrapper LONETILDE = wrap("(?:~>?)");

	public static final REWrapper TILDETRIM = wrap(
			"(\\s*)" + LONETILDE + "\\s+", true);

	public static final String TILDETRIM_REPLACE = "$1~";

	public static final REWrapper TILDE = wrap(
			"^" + LONETILDE + XRANGEPLAIN + "$");

	public static final REWrapper TILDELOOSE = wrap(
			"^" + LONETILDE + XRANGEPLAINLOOSE + "$");

	// Caret ranges.
	// Meaning is "at least and backwards compatible with"
	public static final REWrapper LONECARET = wrap("(?:\\^)");

	public static final REWrapper CARETTRIM = wrap(
			"(\\s*)" + LONECARET + "\\s+", true);

	public static final String CARETTRIM_REPLACE = "$1^";

	public static final REWrapper CARET = wrap(
			"^" + LONECARET + XRANGEPLAIN + "$");
	public static final REWrapper CARETLOOSE = wrap(
			"^" + LONECARET + XRANGEPLAINLOOSE + "$");

	// A simple gt/lt/eq thing, or just "" to indicate "any version"
	public static final REWrapper COMPARATORLOOSE = wrap(
			"^" + GTLT + "\\s*(" + LOOSEPLAIN + ")$|^$");
	public static final REWrapper COMPARATOR = wrap(
			"^" + GTLT + "\\s*(" + FULLPLAIN + ")$|^$");


	// An expression to strip any whitespace between the gtlt and the thing
	// it modifies, so that `> 1.2.3` ==> `>1.2.3`
	public static final REWrapper COMPARATORTRIM = wrap(
			"(\\s*)" + GTLT +
			"\\s*(" + LOOSEPLAIN + "|" + XRANGEPLAIN + ")", true);

	// this one has to use the /g flag

	public static final String COMPARATORTRIM_REPLACE = "$1$2$3";

	// Something like `1.2.3 - 1.2.4`
	// Note that these all use the loose form, because they"ll be
	// checked against either the strict or loose comparator form
	// later.
	public static final REWrapper HYPHENRANGE = wrap(
			"^\\s*(" + XRANGEPLAIN + ")" +
			"\\s+-\\s+" +
			"(" + XRANGEPLAIN + ")" +
			"\\s*$");

	public static final REWrapper HYPHENRANGELOOSE = wrap(
			"^\\s*(" + XRANGEPLAINLOOSE + ")" +
			"\\s+-\\s+" +
			"(" + XRANGEPLAINLOOSE + ")" +
			"\\s*$");

	// Star ranges basically just allow anything at all.
	public static final REWrapper STAR = wrap("(<|>)?=?\\s*\\*");

}
