JXPlorer i18n Translation Files
===============================

JX.properties contains a list of all the keywords in JXplorer (as of 22 Feb 03).
To Translate into another language, the phrases on the RIGHT HAND SIDE of the
equals sign must be translated.  The format should be either UTF8 or Unicode,
NOT a local encoding such as Shift-JIS, Big-5 Chinese or whatever.  I have 
rewritten the java language handling so that normal utf-8 and unicode can
be used, rather than the ludicrous java 'escaped unicode' format (although
that should work as well).

Some out of date japanese translation files are included as examples, but
these don't actually work - the larger JX.properties file has to be used
instead.

The files follow the normal java i18n standard for locale specification:
e.g. 
JX.properties - Default
JX_de.properties - German
JX_en.properties - English
JX_fr.properties - French
JX_ja.properties - Japanese
JX_zh_CN.properties - Simplified Chinese
JX_zh_TW.properties - Traditional Chinese
JX_en_US.properties - wierd american dialect of english


   cheers!

           - Chris
