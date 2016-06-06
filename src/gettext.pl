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

use Cwd;
$root_dir = cwd();

$verbose;

open(OUTPUTFILE, ">i18n.txt") or die ("Can't open output file i18n.txt - *confused*\n");

parse_command_line_args();	# may modify $directory

read_directory($directory); # runs the command



# parse optional command line arguments 

sub parse_command_line_args
{
	foreach $argnum (0 .. $#ARGV) 
	{
	    $arg = $ARGV[$argnum];

print "parsing argument $arg\n";
	    
	    if ($arg =~ /-h.*/ )
	    {
	    	print_help();
	    	exit;
	   	} 	
	   	
	    if ($arg =~ /-v.*/ )
	    {
	    	$verbose = true;
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
	    		print("Error - no directory found after -f option\n");
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
	" statements, extracting the strings into the file i18n.txt.\n\n" .
	" USAGE:\n" .
	" perl gettext.pl [-v|-verbose] [-h|-help] [-d <root directory>] [-f <file extension>] .\n" .
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
		if ($verbose) {print ("READING DIRECTORY $dir\n");}	
	}
	else
	{
		if ($verbose) {print ("  - error: can't find directory $dir\n");}
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

	if ($verbose) {print ("  processing $file\n");}

    while ($line = <JAVAFILE>)  
    {
        
       while ( $line =~ /CBIntText.get\(\"(.*?)\"\)/g )
       {
            print OUTPUTFILE $1 . "\n";
       }
    }                    
}

