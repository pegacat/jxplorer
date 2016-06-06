#!/usr/local/bin/perl


print ("\nGetText 1.0   (Chris Betts '03)\n");

#
#  GetText
#
#  GetText recursively processes all files in a complete directory
#  tree, extracting strings from 'CBIntText.get("[string]")' statements.

#  This perl script is deliberately long-winded and commented, in 
#  order to make it understandable by perl newbies (like me).

# get root directory to map

$directory = ".";  # XXX Can't get this to work ???

$original = "*";

$outputfile = "JX.properties"; #default

%seen = ();    # hash of values already encountered
@biglist = (); # list of values for translation
$doubleList = 1;  #  produce a key = key file; or just have keys on left hand side if false...


use Cwd;
$root_dir = cwd();

$verbose;
$silent;


parse_command_line_args();	# may modify $directory

open(OUTPUTFILE, ">" . $outputfile) or die ("Can't open output file '$outputfile' - *confused*\n");


read_directory($directory); # runs the command

print_list();


if (!$silent) {print "\nfinished: output written to output file $outputfile\n"};

sub print_list
{
    #TODO: it would be nice to have a timestamp in here...
    print OUTPUTFILE "#JXplorer Translation File: ", scalar localtime, "\n\n";

    my @sorted_list = sort(@biglist);

    foreach $word (@sorted_list)
    {
        if ($verbose) {print "final: " . $word;}
        print OUTPUTFILE $word;
    }

    close(OUTPUTFILE);
}


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
	   	
	    if ($arg =~ /-v.*/ )
	    {
	    	$verbose = true;
	   	} 	

		if ($arg =~ /-s.*/ )
	    {
	    	$silent = true;
	   	}

	    if ($arg =~ /\-d/)
	    {
	    	if ($#ARGV == $argnum)
	    	{
	    		print("Error - no directory found after -d option\n");
	    		print ("(only $argnum arguments found)");
	    		exit();	
	    	}	
	    	$argnum++;
	    	$directory = $ARGV[$argnum];
	    }
	    if ($arg =~ /\-f/)
	    {
	    	if ($#ARGV == $argnum)
	    	{
	    		print("Error - no file extension found after -f option\n");
	    		print ("(only $argnum arguments found)");
	    		exit();	
	    	}	
	    	$argnum++;
	    	$original = $ARGV[$argnum];
	    }
	}	
}


# Print out a simple unix-style help message 

sub print_help
{
	print ("\n" .
	" Searches files in a directory tree for CBIntText.get(\"\")\n" .
	" statements, extracting the strings into the file JX.properties.\n" .
	" WARNING: Must use full path for directory walking to work (!?!).\n" .
	" USAGE:\n" .
	" perl gettext.pl [-v|-verbose] [-s| -silent] [-h|-help] [-d <root directory>] [-f <file extension>] .\n" .
	" e.g. perl gettext.pl -d /src/backup -f java \n" .
	" e.g. perl gettext.pl -v -d /Users/chrisbetts/projects/jxplorer/src \n" .
	" (nb - must be run from it's own root directory\n");
}


# Start reading files and recursing through directories

sub read_directory
{
	my ($dir);                 # verbose local variable for clarity...
	my ($moving_file);
	my ($dir_candidate);
	my (@dir_list);
	
	$dir = $_[0];
#	if ($dir eq ".")
#	{
#		if ($verbose) {print ("READING CURRENT DIRECTORY\n");}	
#	    chdir;
#	}
#	elsif (chdir($dir))
    if (chdir($dir))
	{
		if (!$silent) {print ("==> READING DIRECTORY $dir\n");}
	}
	else
	{
		if (!$silent) {print ("  - error: can't find directory $dir\n");}
		return;
	}
	
	# read all the files in this directory
	while ( defined($touch_file = glob("*.$original")) )
	{
		parse_file($touch_file);	
	}
		
	# read all the sub directories in this directory
	while ( defined($dir_candidate = glob("*")) )
	{
		if (-d $dir_candidate)	# create a local (my()) list, to avoid the glob(.) 
		{                       # variable being mushed between recursive calls...
			@dir_list = (@dir_list, $dir_candidate);
		}
	}		

	foreach $dir_candidate (@dir_list)
	{	
		read_directory ($dir . "/" . $dir_candidate);	
	}
	
}

sub parse_file
{
    # For each file...

    $file = $_[0];
    
    open(JAVAFILE, "$file") or die ("Can't open input file $file \n");

	if (!$silent) {print ("   ==> processing $file\n");}

    while ($line = <JAVAFILE>)  
    {
        
       while ( $line =~ /CBIntText.get\(\"(.*?)\"/g )
       {

            if ($verbose) {print "\n\nfound line: " . $line;}

            #print OUTPUTFILE $1 . "\n";
            #@words = m/.*?CBIntText.get\(\"(.*?)\"/sg;	# WARNING - We're not handling embedded double quotes.  (We don't have any in JX at the moment :-) ).
            #@words = ($x =~ /(\w+)/g);
            $search = $line;
            @words = ($search =~ /CBIntText\.get\(\"(.*?)\"/g);	# WARNING - We're not handling embedded double quotes.  (We don't have any in JX at the moment :-) ).
            $first = 1;
            foreach $word (@words)
            {
                if ($verbose)
                {
                    if ($first)
                    {
                        print ("words: " . $word);
                        $first = 0;
                    }
                    else
                    {
                        print (", " . $word);
                    }
                }
                addWord($word);
            }
       }
    }                    
}



sub addWord
{
    # For each word

    $word = $_[0];

	if (length($word) > 0)
	{
    	$temp = $word;					# use to produce 'english' properties file
	    $word =~ s/ /\\ /sg;            # java properties file requires left hand side
	    $word =~ s/=/\\=/sg;            # to escape some stuff, for wierd java reasons.
		$word =~ s/^\s+//;              # (did they screw up properties files or what!!!)
		$word =~ s/\s+$//;

		if ($doubleList)
		{
    	    $word .= "=" . $temp . "\n";   # use to produce 'english' properties file (with translations just copied from left side)
		}
		else
		{
    	    $word .= " = \n";
		}

    	unless( $seen{$word})
    	{
    	    $seen{$word} = 1;
    	    push (@biglist, $word);
    	}
    }
}