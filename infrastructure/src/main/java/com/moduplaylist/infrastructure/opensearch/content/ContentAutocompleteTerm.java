package com.moduplaylist.infrastructure.opensearch.content;

public record ContentAutocompleteTerm(String text, String compactText, String type) {

	public ContentAutocompleteTerm(String text, String type) {
		this(text, removeWhitespace(text), type);
	}

	private static String removeWhitespace(String text) {
		if (text == null) {
			return null;
		}
		StringBuilder compact = new StringBuilder(text.length());
		text.codePoints()
			.filter(codePoint -> !Character.isWhitespace(codePoint))
			.forEach(compact::appendCodePoint);
		return compact.toString();
	}
}
