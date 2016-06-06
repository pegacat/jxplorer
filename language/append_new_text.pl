#!/usr/local/bin/perl

print ("\nAppend New Text 1.0   (Chris Betts '12)\n");

#
#  Append New Text
#
#  Takes an existing translation file of key = value unicode pairs,
# and merges it with a 'new' set of values to translate.

#  This perl script is deliberately long-winded and commented, in 
#  order to make it understandable by perl newbies (like me).

# get root directory to map

$directory = ".";  # XXX Can't get this to work ???

$originalfile = 0;

$newfile = "";

%new = ();     # new keys
%old = ();     # old keys - no longer needed
%current = (); # currently translated keys...

use Cwd;
$root_dir = cwd();

$verbose;

$output_file = "JX_new.properties";


parse_command_line_args();	# may modify $directory

parse_files(); # runs the command

printOutputFiles();


# parse optional command line arguments 

sub parse_command_line_args
{
	foreach $argnum (0 .. $#ARGV) 
	{
	    $arg = $ARGV[$argnum];

	    if ($arg =~ /-h.*/ )
	    {
	    	print_help();
	    	exit;
	   	} 	
	    elsif ($arg =~ /-v.*/ )
	    {
	    	$verbose = true;
	   	}

	   	# don't know how to do perl loops properly; need to skip one forward in argument list :-(.
	   	#
	    #elsif ($arg =~ /\-o/)
	    #{
	    #    print $argnum . " found o\n";
	   # 	if ($#ARGV == $argnum)
	    #	{
	   # 		print("Error - no output file found after -o option\n");
	   # 		print ("(only $argnum arguments found)");
	   #		exit();
	   #	}
	   # 	$argnum++;
	   # 	$output_file = $ARGV[$argnum];
       #     print $argnum . " setting output file to: " . $ARGV[$argnum] . "\n";
	   # }
	    elsif ($originalfile)
        {
            $newfile = $arg;
        }
        else
        {
            $originalfile = $arg;
	    }
	}

}


# Print out a simple unix-style help message 

sub print_help
{
	print ("\n" .
	" Merges an existing language file with a new (presumably more\n" .
	" extensive) language file.  Useful when new versions come out\n" .
	" which have more strings to translate...\n" .
	" perl append_new_text.pl [-v|-verbose] [-h|-help] [-0|-output <outputfile>] old_file new_file\n" .
	" e.g. perl append_new_text.pl -v JX_hu.properties JX.properties \n" .
	" (outputs to new i18n.txt file)\n");
}


# Start reading files and recursing through directories

sub parse_files
{
    print "merging original file: " . $originalfile . " with new file: " . $newfile . " to create output file: " . $output_file . "\n";

    open(ORIGINALFILE, "$originalfile") or die ("Can't open original file for input $originalfile \n");

	if ($verbose) {print ("  processing original text from: $originalfile\n\n");}

    # load up values from old file (usually the existing translation)
    while ($line = <ORIGINALFILE>)
    {
       $line = trim($line);
       $line =~ /(.*?)=(.*?)/;
       $key = trim($1);
       $newLine = $line . "\n";

       if ($verbose) {print "original: " . $newLine;}

       unless( $current{$key})
    	{
    	    $old{$key} = $newLine;
    	    $current{$key} = $newLine;
    	}
    }
    close(ORIGINALFILE);

    # load up new values (and discover values in old translation no longer needed...)

	if ($verbose) {print ("\n  processing new text from: $newfile\n\n");}

    open(NEWFILE, "$newfile") or die ("Can't open new file for input $newfile \n");


    while ($line = <NEWFILE>)  
    {
       $line = trim($line);
       $line =~ /(.*?)=(.*?)/;
       $key = trim($1);
       $newLine = $line . "\n";

       if ($verbose) {print "new: " . $newLine;}

        if ($current{$key})
        {
    	    delete($old{$key}); # remove values that are still around from the 'obsolete' list
        }
        else
        {
    	    $new{$key} = $newLine;
        }
    }

    # Remove keys that are in the 'current' list, but not in the 'new' list; they are obsolete
    foreach $k (keys (%old))
    {
        if ($verbose) {print "deleting: " . $k . " => " . $current{$k} . "\n";}
        delete($current{$k});
    }


    close(NEWFILE);
}

sub printOutputFiles
{
    open(OUTPUTFILE, ">" . $output_file) or die ("Can't open output file $output_file - *confused*\n");

    print OUTPUTFILE "#JXplorer Translation File: Updated ", scalar localtime, "\n\n";


    # Print out unchanged, existing translations

    my @currentWords = values %current;
    my @sorted_current_list = sort(@currentWords);
    print OUTPUTFILE "#Current:\n\n";
    print OUTPUTFILE @sorted_current_list;

    if ($verbose) {
        print  "#Current:\n\n";
        print  @sorted_current_list;
    }

    # print out new strings that need to be translated...

    my @newWords = values %new;
    my @sorted_new_list = sort(@newWords);
    print OUTPUTFILE "\n\n#Strings From New Version:\n\n";
    print OUTPUTFILE @sorted_new_list;

    if ($verbose) {
        print  "\n\n#Strings From New Version:\n\n";
        print  @sorted_new_list;
    }

    # print out separate file of strings no longer used (useful for checking minor
    # changes, such as punctuation marks)

    my @oldWords = values %old;
    my @sorted_old_list = sort(@oldWords);
    print OUTPUTFILE "\n\n#Values no Longer Used (may be deleted):\n\n";
    print OUTPUTFILE @sorted_old_list;

    if ($verbose) {
        print  "\n\n#Values no Longer Used:\n\n";
        print  @sorted_old_list;
    }

    close(OUTPUTFILE);
}

sub trim($)
{
    my $string = shift;
    $string =~ s/^\s+//;
    $string =~ s/\s+$//;
    return $string;
}

