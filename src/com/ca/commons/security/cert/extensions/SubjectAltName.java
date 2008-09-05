
package com.ca.commons.security.cert.extensions;

import com.ca.commons.security.asn1.*;

/**
 * <pre>
 *  SubjectAltName ::= GeneralNames
 *
 *  GeneralNames ::= SEQUENCE SIZE (1..MAX) OF GeneralName
 *
 *  GeneralName ::= CHOICE {
 *       otherName                       [0]     OtherName,
 *       rfc822Name                      [1]     IA5String,
 *       dNSName                         [2]     IA5String,
 *       x400Address                     [3]     ORAddress,
 *       directoryName                   [4]     Name,
 *       ediPartyName                    [5]     EDIPartyName,
 *       uniformResourceIdentifier       [6]     IA5String,
 *       iPAddress                       [7]     OCTET STRING,
 *       registeredID                    [8]     OBJECT IDENTIFIER}
 *
 *  OtherName ::= SEQUENCE {
 *       type-id    OBJECT IDENTIFIER,
 *       value      [0] EXPLICIT ANY DEFINED BY type-id }
 *
 *  EDIPartyName ::= SEQUENCE {
 *       nameAssigner            [0]     DirectoryString OPTIONAL,
 *       partyName               [1]     DirectoryString }
 * </pre>
 *
 * @author vbui
 */
public class SubjectAltName extends IssuerAltName
{
}

