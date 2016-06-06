package com.ca.commons.naming;

/**
 * Enumerates the types of Ldif Change entries; e.g.
 * <pre>
 * changetype: add
 * changetype: modify
 * changetype: delete
 * changetype: modrdn
 * changetype: moddn
 * </pre>
 */
public enum LdifEntryType
{
    normal, add, modify, delete, modrdn, moddn;
}
