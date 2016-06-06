#!/bin/sh
#
#  batch script to create 'updated' language files.
#
#  (You will need to adjust the file path in the first 'extract_terms' command)
#
perl extract_terms.pl -d ~/projects/jxplorer/src/com/ca -f java

perl append_new_text.pl JX_fr.properties JX.properties
cp JX_new.properties JX_fr_new.properties

perl append_new_text.pl JX_de.properties JX.properties
cp JX_new.properties JX_de_new.properties

perl append_new_text.pl JX_hu.properties JX.properties
cp JX_new.properties JX_hu_new.properties

perl append_new_text.pl JX_zh_CN.properties JX.properties
cp JX_new.properties JX_zh_CN_new.properties

perl append_new_text.pl JX_zh_TW.properties JX.properties
cp JX_new.properties JX_zh_TW_new.properties
