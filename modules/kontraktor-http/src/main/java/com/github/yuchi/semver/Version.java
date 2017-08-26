package com.github.yuchi.semver;

import java.util.regex.Matcher;

public class Version implements Comparable<Version> {

	public static Version from(Object obj, boolean loose) {
		if (obj == null) {
			return null;
		}
		else if (obj instanceof Version) {
			Version version = (Version)obj;

			if (version.loose == loose) {
				return version;
			}
			else {
				return new Version(version.version, loose);
			}
		}
		else {
			try {
				return new Version(String.valueOf(obj), loose);
			}
			catch (IllegalArgumentException iae) {
				return null;
			}
		}
	}

	public Version(String raw) throws IllegalArgumentException {
		this(raw, false);
	}

	public Version(String raw, boolean loose)
		throws IllegalArgumentException {

		this.raw = raw;
		this.loose = loose;

		REWrapper re = loose ? Constants.LOOSE : Constants.FULL;

		Matcher m = re.match(raw.trim());

		if (!m.matches()) {
			throw new IllegalArgumentException("Invalid Version: " + raw);
		}

		this.major = Integer.parseInt(m.group(1));

		if ((this.major < 0) || (this.major == Integer.MAX_VALUE)) {
			throw new IllegalArgumentException("Invalid Major Version");
		}

		this.minor = Integer.parseInt(m.group(2));

		if ((this.minor < 0) || (this.minor == Integer.MAX_VALUE)) {
			throw new IllegalArgumentException("Invalid Minor Version");
		}

		this.patch = Integer.parseInt(m.group(3));

		if ((this.patch < 0) || (this.patch == Integer.MAX_VALUE)) {
			throw new IllegalArgumentException("Invalid Patch Version");
		}

		String pre = m.group(4);

		if ((pre == null) || pre.isEmpty()) {
			this.prerelease = new Object[0];
		}
		else {
			String[] parts = pre.split("\\.");

			this.prerelease = new Object[parts.length];

			for (int i = 0; i < parts.length; ++i) {
				String part = parts[i];

				if (Constants.NUMERIC.matches(part)) {
					this.prerelease[i] = Integer.parseInt(part);
				}
				else {
					this.prerelease[i] = part;
				}

			}
		}

		String b = m.group(5);

		if ((b == null) || b.isEmpty()) {
			this.build = new String[0];
		}
		else {
			this.build = b.split("\\.");
		}

		StringBuilder sb = new StringBuilder();

		sb.append(major);
		sb.append(".");
		sb.append(minor);
		sb.append(".");
		sb.append(patch);

		if (this.prerelease.length > 0) {
			sb.append("-");

			String[] parts = new String[this.prerelease.length];

			for (int i = 0; i < this.prerelease.length; ++i) {
				parts[i] = String.valueOf(this.prerelease[i]);
			}

			sb.append(String.join(".", parts));
		}

		this.version = sb.toString();
	}

	public int getMajor() {
		return this.major;
	}

	public int getMinor() {
		return this.minor;
	}

	public int getPatch() {
		return this.patch;
	}

	public Object[] getPrerelease() {
		return this.prerelease;
	}

	public String[] getBuild() {
		return this.build;
	}

	@Override
	public boolean equals(Object obj) {
		if ((obj != null) && (obj instanceof Version)) {
			Version other = (Version)obj;

			return other.version.equals(this.version);
		}
		else {
			return false;
		}
	}

	@Override
	public int compareTo(Version other) {
		// Standard M.m.p compare

		if (this.major > other.major) {
			return 1;
		}

		if (this.major < other.major) {
			return -1;
		}

		if (this.minor > other.minor) {
			return 1;
		}

		if (this.minor < other.minor) {
			return -1;
		}

		if (this.patch > other.patch) {
			return 1;
		}

		if (this.patch < other.patch) {
			return -1;
		}

		// Prerelease pre-check

		// NOT having a prerelease is > having one

		if ((this.prerelease.length == 0) && (other.prerelease.length > 0)) {
			return 1;
		}

		if ((this.prerelease.length > 0) && (other.prerelease.length == 0)) {
			return -1;
		}

		if ((this.prerelease.length == 0) && (other.prerelease.length == 0)) {
			return 0;
		}

		// Prerelease compare

		int i = 0;

		do {
			Object a = (i >= this.prerelease.length)
				? null : this.prerelease[i];
			Object b = (i >= other.prerelease.length)
				? null : other.prerelease[i];

			if ((a == null) && (b == null)) {
				return 0;
			}
			else if (b == null) {
				return 1;
			}
			else if (a == null) {
				return -1;
			}
			else if (a.equals(b)) {
				continue;
			}
			else {
				return compareIdentifiers(a, b);
			}
		}
		while (i++ >= 0);

		return 0;
	}

	@Override
	public String toString() {
		return version;
	}

	protected static int compareIdentifiers(Object a, Object b) {
		boolean anumeric = a instanceof Integer;
		boolean bnumeric = b instanceof Integer;

		if (anumeric && bnumeric) {
			return ((Integer)a).compareTo((Integer)b);
		}
		else if (!anumeric && !bnumeric) {
			return ((String)a).compareTo((String)b);
		}
		else if (anumeric) {
			return -1;
		}
		else /*if (bnumeric)*/ {
			return 1;
		}
	}

	protected final int major;
	protected final int minor;
	protected final int patch;
	protected final Object[] prerelease;
	protected final String[] build;
	protected final String raw;
	protected final String version;
	protected final boolean loose;

}
