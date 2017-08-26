package com.github.yuchi.semver;

import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class REWrapper {

	protected REWrapper(String raw) {
		this(raw, false);
	}

	protected REWrapper(String raw, boolean global) {
		_raw = raw;
		_global = global;
	}

	public Matcher match(CharSequence input) {
		return getPattern().matcher(input);
	}

	public boolean matches(CharSequence input) {
		return Pattern.matches(_raw, input);
	}

	public String replace(String input, String replacement) {
		if (_global) {
			return input.replaceAll(_raw, replacement);
		}
		else {
			return input.replaceFirst(_raw, replacement);
		}
	}

	public String replace(String input, Function<Matcher, StringBuilder> fn) {
		Matcher m = getPattern().matcher(input);

		StringBuilder sb = new StringBuilder();

		int start = 0;

		while (m.find()) {
			sb.append(input.substring(start, m.start()));

			sb.append(fn.apply(m));

			start = m.end();
		}

		sb.append(input.substring(start));

		return sb.toString();
	}

	public Stream<String> replaceWithTokensStream(String input, Function<Matcher, StringBuilder> fn) {
		Matcher m = getPattern().matcher(input);

		StringBuilder sb = new StringBuilder();

		int start = 0;

		while (m.find()) {
			sb.append(input.substring(start, m.start()));

			sb.append(fn.apply(m));

			start = m.end();
		}

		sb.append(input.substring(start));

		return Arrays.stream(sb.toString().split(" "));
	}

	public Pattern getPattern() {
		if (_pattern == null) {
			_pattern = Pattern.compile(_raw);
		}

		return _pattern;
	}

	@Override
	public String toString() {
		return _raw;
	}

	private boolean _global = false;
	private Pattern _pattern = null;
	private String _raw = null;

}
