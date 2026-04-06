package se.ohdeere.nightscout.service.auth;

import java.util.List;

/**
 * Resolved authentication context for a Nightscout request.
 */
public record NightscoutAuth(String subject, List<String> permissions, boolean admin) {

	public static final NightscoutAuth ANONYMOUS = new NightscoutAuth("anonymous", List.of(), false);

	public static NightscoutAuth adminAuth(String subject) {
		return new NightscoutAuth(subject, List.of("*:*:*"), true);
	}

	public boolean hasPermission(String collection, String action) {
		if (this.admin) {
			return true;
		}
		for (String perm : this.permissions) {
			if (matchesPermission(perm, collection, action)) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchesPermission(String permission, String collection, String action) {
		String[] parts = permission.split(":");
		if (parts.length < 3) {
			return false;
		}
		return (parts[0].equals("*") || parts[0].equals("api")) && (parts[1].equals("*") || parts[1].equals(collection))
				&& (parts[2].equals("*") || parts[2].equals(action));
	}

}
