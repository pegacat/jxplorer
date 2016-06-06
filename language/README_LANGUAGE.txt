JXPlorer i18n Translation Files
===============================

JX.properties contains a list of all the keywords in JXplorer.
To Translate into another language, the phrases on the RIGHT HAND SIDE of the
equals sign must be translated.  The format should be either UTF8 or Unicode,
NOT a local encoding such as Shift-JIS, Big-5 Chinese or whatever.  I have 
rewritten the java language handling so that normal utf-8 and unicode can
be used, rather than the ludicrous java 'escaped unicode' format (although
that should work as well).

The files follow the normal java i18n standard for locale specification:
e.g. 
JX.properties - Default
JX_de.properties - German
JX_en.properties - English
JX_fr.properties - French
JX_ja.properties - Japanese
JX_zh_CN.properties - Simplified Chinese
JX_zh_TW.properties - Traditional Chinese
JX_en_US.properties - American dialect of english


   cheers!

           - Chris

Notes for developers:
=====================

There are some perl scripts included here that can be used to
build the base properties file, or to find newly added terms not present in
existing translation files.

The current process is to use extract_text.pl to get the CBIntText.get(xxx)
terms from the source to produce an 'english' properties file, and
then 'append_new_text.pl' to merge this text with existing translation files.

e.g.
perl extract_terms.pl -d ~/projects/jxplorer/src/com/ca -f java
perl append_new_text.pl JX_fr.properties JX.properties

