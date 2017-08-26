package com.github.yuchi.semver;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class Range {

	public static Range from(Object obj, boolean loose) {
		if (obj == null) {
			return null;
		}
		else if (obj instanceof Range) {
			Range range = (Range)obj;

			if (range.loose == loose) {
				return range;
			}
			else {
				return new Range(range.raw, loose);
			}
		}
		else {
			try {
				return new Range(String.valueOf(obj), loose);
			}
			catch (IllegalArgumentException iae) {
				return null;
			}
			catch (IllegalStateException ise) {
				return null;
			}
		}
	}

	public Range(String raw) throws IllegalArgumentException {
		this(raw, false);
	}

	public Range(String raw, boolean loose) throws IllegalArgumentException {
		this.raw = raw;
		this.loose = loose;
		this.set = new ArrayList<List<Comparator>>();

		String[] ranges = this.raw.trim().split("\\s*\\|\\|\\s*");

		if (ranges.length == 0) {
			ranges = new String[] {""};
		}

		for (String range : ranges) {
			range = range.trim();

			List<Comparator> comparators = parseRange(range);

			if (!comparators.isEmpty()) {
				this.set.add(comparators);
			}
		}

		if (this.set.isEmpty()) {
			throw new IllegalStateException();
		}
	}

	public boolean isOutside(Version version, Direction dir) {

		if (this.test(version)) {
			return false;
		}

		// From now on, variable terms are as if we're in "gtr" mode.
		// but note that everything is flipped for the "ltr" function.

		Predicate<Integer> gt;
		Predicate<Integer> lt;
		Predicate<Integer> lte;
		Operator op;
		Operator ope;

		if (dir == Direction.HIGH) {
			gt = n -> n > 0;
			lt = n -> n < 0;
			lte = n -> n <= 0;
			op = Operator.GT;
			ope = Operator.GTE;
		}
		else if (dir == Direction.LOW) {
			gt = n -> n < 0;
			lt = n -> n > 0;
			lte = n -> n >= 0;
			op = Operator.LT;
			ope = Operator.LTE;
		}
		else {
			throw new IllegalArgumentException();
		}

		for (List<Comparator> comparators : this.set) {
			Comparator high = null;
			Comparator low = null;

			for (Comparator comparator : comparators) {
				if (comparator.operator == Operator.ANY) {
					comparator = new Comparator(">=0.0.0");
				}

				if (high == null) {
					high = comparator;
				}

				if (low == null) {
					low = comparator;
				}

				int compare = comparator.version.compareTo(high.version);

				if (gt.test(compare)) {
					high = comparator;
				}
				else if (lt.test(compare)) {
					low = comparator;
				}
			}

			// If the edge version comparator has a operator then our version
			// isn't outside it

			if ((high.operator == op) || (high.operator == ope)) {
				return false;
			}

			// If the lowest version comparator has an operator and our version
			// is less than it then it isn't higher than the range

			int compare = version.compareTo(low.version);

			if (((low.operator == Operator.EQ) || (low.operator == op)) &&
				lte.test(compare)) {

				return false;
			}
			else if ((low.operator == ope) && lt.test(compare)) {
				return false;
			}
		}

		return true;
	}

	public boolean test(Version version) {
		for (List<Comparator> comparators : this.set) {
			if (testList(version, comparators)) {
				return true;
			}
		}

		return false;
	}

	protected List<Comparator> parseRange(String range) {
		range = replaceHyphens(range);

		range = Constants.COMPARATORTRIM.replace(
			range, Constants.COMPARATORTRIM_REPLACE);

		range = Constants.TILDETRIM.replace(
			range, Constants.TILDETRIM_REPLACE);

		range = Constants.CARETTRIM.replace(
			range, Constants.CARETTRIM_REPLACE);

		range = range.replaceAll("\\s+", " ");

		REWrapper compRe = this.loose
			? Constants.COMPARATORLOOSE : Constants.COMPARATOR;

		Stream<String> comps = Stream.of(range)
			.flatMap(this::splitBySpace)
			.flatMap(this::replaceCarets)
			.flatMap(this::splitBySpace)
			.flatMap(this::replaceTildes)
			.flatMap(this::splitBySpace)
			.flatMap(this::replaceXRanges)
			.flatMap(this::splitBySpace)
			.map(this::replaceStars);

		if (this.loose) {
			comps = comps.filter(compRe::matches);
		}

		return comps
			.map(comp -> new Comparator(comp, this.loose))
			.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
	}

	protected boolean testList(Version version, List<Comparator> comparators) {
		for (Comparator comparator : comparators) {
			if (!comparator.test(version)) {
				return false;
			}
		}

		if (version.prerelease.length > 0) {
			// Find the set of versions that are allowed to have prereleases
			// For example, ^1.2.3-pr.1 desugars to >=1.2.3-pr.1 <2.0.0
			// That should allow `1.2.3-pr.2` to pass.
			// However, `1.2.4-alpha.notready` should NOT be allowed,
			// even though it's within the range set by the comparators.
			for (Comparator comparator : comparators) {
				if (comparator.operator == Operator.ANY) {
					continue;
				}

				if (comparator.version.prerelease.length > 0) {
					Version allowed = comparator.version;
					if ((allowed.major == version.major) &&
						(allowed.minor == version.minor) &&
						(allowed.patch == version.patch)) {

						return true;
					}
				}
			}

			// Version has a -pre, but it's not one of the ones we like.
			return false;
		}

		return true;
	}

	protected String replaceHyphens(String range) {
		REWrapper hr = this.loose
			? Constants.HYPHENRANGELOOSE : Constants.HYPHENRANGE;

		return hr.replace(range, m -> {
			String from = m.group(1);
			String fromMajor = m.group(2);
			String fromMinor = m.group(3);
			String fromPatch = m.group(4);
			/*String fromPR = m.group(5);
			String fromBuild = m.group(5);*/
			String to = m.group(7);
			String toMajor = m.group(8);
			String toMinor = m.group(9);
			String toPatch = m.group(10);
			String toPR = m.group(11);
			/*String toBuild = m.group(12);*/

			StringBuilder sb = new StringBuilder();

			if (isX(fromMajor)) {
				//
			}
			else if (isX(fromMinor)) {
				sb.append(">=");
				sb.append(fromMajor);
				sb.append(".0.0");
			}
			else if (isX(fromPatch)) {
				sb.append(">=");
				sb.append(fromMajor);
				sb.append(".");
				sb.append(fromMinor);
				sb.append(".0");
			}
			else {
				sb.append(">=");
				sb.append(from);
			}

			if (sb.length() > 0) {
				sb.append(" ");
			}

			if (isX(toMajor)) {
				//
			}
			else if (isX(toMinor)) {
				sb.append("<");
				sb.append(Integer.parseInt(toMajor) + 1);
				sb.append(".0.0");
			}
			else if (isX(toPatch)) {
				sb.append("<");
				sb.append(toMajor);
				sb.append(".");
				sb.append(Integer.parseInt(toMinor) + 1);
				sb.append(".0");
			}
			else if ((toPR != null) && !toPR.isEmpty()) {
				sb.append("<=");
				sb.append(toMajor);
				sb.append(".");
				sb.append(toMinor);
				sb.append(".");
				sb.append(toPatch);
				sb.append("-");
				sb.append(toPR);
			}
			else {
				sb.append("<=");
				sb.append(to);
			}

			return sb;
		});
	}

	protected boolean is0(String identifier) {
		return (identifier.length() == 1) && (identifier.charAt(0) == '0');
	}

	protected boolean isX(String identifier) {
		if ((identifier == null) || identifier.isEmpty()) {
			return true;
		}
		else if (identifier.length() == 1) {
			char c = identifier.charAt(0);

			return (c == 'x') || (c == 'X') || (c == '*');
		}
		else {
			return false;
		}
	}

	protected Stream<String> replaceCarets(String comp) {
		REWrapper re = this.loose ? Constants.CARETLOOSE : Constants.CARET;

		return re.replaceWithTokensStream(comp, m -> {
			StringBuilder sb = new StringBuilder();

			String major = m.group(1);
			String minor = m.group(2);
			String patch = m.group(3);
			String pr = m.group(4);

			if (isX(major)) {
				//
			}
			else if (isX(minor)) {
				sb.append(">=");
				sb.append(major);
				sb.append(".0.0 <");
				sb.append(Integer.parseInt(major) + 1);
				sb.append(".0.0");
			}
			else if (isX(patch)) {
				if (is0(major)) {
					sb.append(">=");
					sb.append(major);
					sb.append(".");
					sb.append(minor);
					sb.append(".0 <"); // SPACE!
					sb.append(major);
					sb.append(".");
					sb.append(Integer.parseInt(minor) + 1);
					sb.append(".0");
				}
				else {
					sb.append(">=");
					sb.append(major);
					sb.append(".");
					sb.append(minor);
					sb.append(".0 <");
					sb.append(Integer.parseInt(minor) + 1);
					sb.append(".0.0");
				}
			}
			else {
				sb.append(">=");
				sb.append(major);
				sb.append(".");
				sb.append(minor);
				sb.append(".");
				sb.append(patch);

				if ((pr != null) && !pr.isEmpty()) {
					if (pr.charAt(0) != '-') {
						sb.append("-");
					}

					sb.append(pr);
				}

				sb.append(" <"); // SPACE!!

				// continues...

				if (is0(major)) {
					if (is0(minor)) {
						sb.append(major);
						sb.append(".");
						sb.append(minor);
						sb.append(".");
						sb.append(Integer.parseInt(patch) + 1);
					}
					else {
						sb.append(major);
						sb.append(".");
						sb.append(Integer.parseInt(minor) + 1);
						sb.append(".0");
					}
				}
				else {
					sb.append(Integer.parseInt(major) + 1);
					sb.append(".0.0");
				}
			}

			return sb;
		});
	}

	protected Stream<String> replaceTildes(String comp) {
		REWrapper re = this.loose ? Constants.TILDELOOSE : Constants.TILDE;

		return re.replaceWithTokensStream(comp, m -> {
			StringBuilder sb = new StringBuilder();

			String major = m.group(1);
			String minor = m.group(2);
			String patch = m.group(3);
			String pr = m.group(4);

			if (isX(major)) {
				//
			}
			else if (isX(minor)) {
				sb.append(">=");
				sb.append(major);
				sb.append(".0.0 <"); // SPACE!
				sb.append(Integer.parseInt(major) + 1);
				sb.append(".0.0");
			}
			else if (isX(patch)) {
				// ~1.2 == >=1.2.0 <1.3.0
				sb.append(">=");
				sb.append(major);
				sb.append(".");
				sb.append(minor);
				sb.append(".0 <"); // SPACE!
				sb.append(major);
				sb.append(".");
				sb.append(Integer.parseInt(minor) + 1);
				sb.append(".0");
			}
			else {
				sb.append(">=");
				sb.append(major);
				sb.append(".");
				sb.append(minor);
				sb.append(".");
				sb.append(patch);

				if ((pr != null) && !pr.isEmpty()) {
					if (pr.charAt(0) != '-') {
						sb.append("-");
					}

					sb.append(pr);
				}

				sb.append(" <");
				sb.append(major);
				sb.append(".");
				sb.append(Integer.parseInt(minor) + 1);
				sb.append(".0");
			}

			return sb;
		});
	}

	protected Stream<String> replaceXRanges(String comp) {
		REWrapper re = this.loose ? Constants.XRANGELOOSE : Constants.XRANGE;

		return re.replaceWithTokensStream(comp, m -> {
			StringBuilder sb = new StringBuilder();

			String gtlt = m.group(1);
			String major = m.group(2);
			String minor = m.group(3);
			String patch = m.group(4);
			/*String pr = m.group(5);*/

			boolean xMajor = isX(major);
			boolean xMinor = xMajor || isX(minor);
			boolean xPatch = xMinor || isX(patch);
			boolean xAny = xPatch;

			if ((gtlt != null) && gtlt.equals("=") && xAny) {
				gtlt = "";
			}

			if (xMajor) {
				if (gtlt.equals("<") || gtlt.equals(">")) {
					// nothing is allowed
					sb.append("<0.0.0");
				}
				else {
					// nothing is forbidden
					sb.append("*");
				}
			}
			else if ((gtlt != null) && !gtlt.isEmpty() && xAny) {
				if (xMinor) {
					minor = "0";
				}

				if (xPatch) {
					patch = "0";
				}

				if (gtlt.equals(">")) {
					// >1 => >=2.0.0
					// >1.2 => >=1.3.0
					// >1.2.3 => >= 1.2.4

					gtlt = ">=";

					if (xMinor) {
						major = String.valueOf(Integer.parseInt(major) + 1);
						minor = "0";
						patch = "0";
					}
					else if (xPatch) {
						minor = String.valueOf(Integer.parseInt(minor) + 1);
						patch = "0";
					}
				}
				else if (gtlt.equals("<=")) {
					// <=0.7.x is actually <0.8.0, since any 0.7.x should
					// pass. Similarly, <=7.x is actually <8.0.0, etc.

					gtlt = "<";

					if (xMinor) {
						major = String.valueOf(Integer.parseInt(major) + 1);
					}
					else {
						minor = String.valueOf(Integer.parseInt(minor) + 1);
					}
				}

				sb.append(gtlt);
				sb.append(major);
				sb.append(".");
				sb.append(minor);
				sb.append(".");
				sb.append(patch);
			}
			else if (xMinor) {
				sb.append(">=");
				sb.append(major);
				sb.append(".0.0 <");
				sb.append(Integer.parseInt(major) + 1);
				sb.append(".0.0");
			}
			else if (xPatch) {
				sb.append(">=");
				sb.append(major);
				sb.append(".");
				sb.append(minor);
				sb.append(".0 <");
				sb.append(major);
				sb.append(".");
				sb.append(Integer.parseInt(minor) + 1);
				sb.append(".0");
			}
			else {
				sb.append(m.group(0));
			}

			return sb;
		});
	}

	protected String replaceStars(String comp) {
		return Constants.STAR.replace(comp.trim(), "");
	}

	protected Stream<String> splitBySpace(String input) {
		return Arrays.stream(input.split(" "));
	}

	protected final boolean loose;
	protected final String raw;
	protected final List<List<Comparator>> set;

}
