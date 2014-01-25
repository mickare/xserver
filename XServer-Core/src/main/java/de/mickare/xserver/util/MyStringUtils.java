package de.mickare.xserver.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyStringUtils {

	private MyStringUtils() {
	}

	public static String trimEnd(String text)
	{
	    return text.replaceAll("[\\s\\n\\r]+$", "");
	}
	
	
	public static String stackTraceToString(Throwable e)
	{
		StringBuilder sb = new StringBuilder();
		for (StackTraceElement element : e.getStackTrace())
		{
			sb.append(element.toString()).append(" (").append(element.getLineNumber()).append(")");
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public static List<String> replace(List<String> list, String target, String replacement) {
		ArrayList<String> result = new ArrayList<String>(list);
		for (int i = 0; i < list.size(); i++) {
			result.set(i, result.get(i).replace(target, replacement));
		}
		return result;
	}

	public static List<String> replace(List<String> list, char target, char replacement) {
		ArrayList<String> result = new ArrayList<String>(list);
		for (int i = 0; i < list.size(); i++) {
			result.set(i, result.get(i).replace(target, replacement));
		}
		return result;
	}

	public static ArrayList<String> splitArrayList(String text, String splitter) {
		ArrayList<String> result = new ArrayList<String>(Arrays.asList(text.split(splitter)));
		if (result.size() > 0) {
			if (result.get(0).isEmpty()) {
				result.remove(0);
			}
		}
		return result;
	}

	public static String getTimeString(long time) {
		long h = time / 3600;
		long m = (time % 3600) / 60;
		long s = time % 60;

		StringBuilder result = new StringBuilder();

		if (h > 0) {
			result.append(h);
			result.append("h");
		}
		if (m > 0) {
			if (h > 0) {
				result.append(" ");
			}
			result.append(m);
			result.append("m");
		}
		if (s > 0) {
			if (h > 0 || m > 0) {
				result.append(" ");
			}
			result.append(s);
			result.append("s");
		}
		return result.toString();
	}

	public static String join(String[] array, String delimiter) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String s : array) {
			if (first) {
				first = false;
			} else {
				sb.append(delimiter);
			}
			sb.append(s);
		}
		return sb.toString();
	}

}
