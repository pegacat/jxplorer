#!/usr/local/bin/perl 

#
#  codestripper  - create a unique list of words and phrases from text containing embedded 'CBIntText.get(...)'
#		   strings.  (produced from grep, multi-file search, or similar, and placed in a single text file)
#
#              hacked together by Chris Betts 9/1/01
#              rehacked 22/2/03

    $inputfile = "i18n.txt";  #default
    $outputfile = "JX.properties"; #default
    
	%seen = ();    # hash of values already encountered
	@biglist = (); # list of values for translation
	$doubleList = 0;  # don't produce a key = key file; just have keys on left hand side...

    parse_command_line_args();
	
	# open the main file of code to strip keywords from

    open(INFILE, "$inputfile") or die ("Can't open file $inputfile - *confused*\n");

	# open output file to stick resulting keyword list in.

    open(OUTFILE, ">$outputfile") or die ("Can't open output file $outputfile - *confused*\n");

    get_words();

    print_list();
    
	close(INFILE);
	close(OUTFILE);		        
    print "\nfinished: output written to output file '$outputfile'\n";
    
sub print_list
{    
    
    print OUTFILE "name = " . (($doubleList)?" default properties\n":"\n");
    print OUTFILE @biglist;
}

#
#  Search for words or phrases enclosed by 'CBIntText.get("...") '.  (Nb - the variable 'word' below
#  is short for 'words and phrases' :-)
#
sub get_words
{
   while ($line = <INFILE>)  
   {
        if ($preprocessed)
        {
            chop($line);
            addWord($line);
        }
        else
        {
            @words = m/.*?CBIntText.get\(\"(.*?)\"\)/sg;	
            foreach $word (@words)
            {
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
    	    $word .= " = " . $temp . "\n";   # use to produce 'english' properties file (with translations just copied from left side)
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
    	
sub print_help
{
   print "USAGE: perl codestripper.pl [-f inputfile] [-k keywords ] [-english] [-verbose] [-help] [-preprocessed] \n" .
         "\n-f The input file is a list of words, produced by grep or similar\n" .
         "   (you can grep for CBIntText.get(\".*?\") ) - default is 'i18n.txt'.\n" .
         "-o the output file - default is 'JX.properties'\n" .
         "-k A diff can be produced using an existing set of keywords\n" .
         "-e an 'english' java properties file (one with words on both sides)\n" .
         "-v verbose, for more debug info \n" .
         "-h this help message \n ".
         "-p preprocessed means the 'CBIntText.get(\"\")' has been stripped already\n\n";
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

		if ($arg =~ /-e.*/)	#producing double key list file
		{
			$doubleList = 1;
			print STDOUT "creating double key list file.\n";				
		}
        if ($arg =~ /\-f/)
	    {
	    	if ($#ARGV == $argnum)
	    	{
	    		print("Error - no file name found after -f option\n");
	    		print ("(only $argnum arguments found)");
	    		exit();	
	    	}	
	    	$argnum++;
	    	$inputfile = $ARGV[$argnum];
	    }
        if ($arg =~ /\-o/)
	    {
	    	if ($#ARGV == $argnum)
	    	{
	    		print("Error - no file name found after -o option\n");
	    		print ("(only $argnum arguments found)");
	    		exit();	
	    	}	
	    	$argnum++;
	    	$outputfile = $ARGV[$argnum];
	    }
        if ($arg =~ /\-k/)
	    {
	        # read a list of ignored phrases (e.g. already translated)
	        
	    	if ($#ARGV == $argnum)
	    	{
	    		print("Error - no file name found after -f option\n");
	    		print ("(only $argnum arguments found)");
	    		exit();	
	    	}	
	    	$argnum++;
	    	$keyfile = $ARGV[$argnum];

			open(EXISTING, "$keyfile") or die ("Can't open existing keywords file '$ARGV[1]'");				
			open(IGNORED, ">ignored.txt") or die ("Unable to open 'ignored.txt' file");
			while (<EXISTING>)
			{
				$_ =~ s/\\=/EQUALS/sg;
				$_ =~ s/(.*? = ).*/$1/sg;
				$_ =~ s/EQUALS/\\=/sg;
				$_ =~ s/^\s+//;
				$_ .= "\n";
				$seen{$_} = 1;
				print IGNORED $_;
			}
			close(IGNORED);
			close(EXISTING);	
	    }
	    if ($arg =~ /-p.*/)
	    {
	        $preprocessed = true;
	    }
	}
}