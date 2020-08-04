#!/usr/bin/perl

use File::Basename;
use warnings;
use Getopt::Long;

sub trim { my $s = shift; $s =~ s/^\s+|\s+$//g; return $s };

my $fixtimes=0;
my $but='';
my $only='';
my $labels='';
GetOptions(
	   'labels=s' => \$labels,
	   'fixtimes' => \$fixtimes,
	   'out=s' => \$out,
	   'only=s' => \$only,
	   'but=s' => \$but
	  );

my $infile = shift;

die "Transforms a file iteratively produced by the method Daidalus.toString into a file that can be processed by the DaidalusFileWalker class.\n".
  "Usage: daidalize [--only <ac1>,..,<acn>] [--but <ac1>,..,<acn>] [--labels <lab1>,..,<labn>] [--out <filename>] [--fixtimes] <file>\n" if !$infile;

open(INFILE,$infile) || die "** Error: Cannot open file $infile\n";

my $outfile;
my $confile;

if ($out) {
  $outfile = "$out.daa";
  $confile = "$out.conf";
} else {
  my ($base,$path,$type) = fileparse($infile,qr{\..+}); 
  $outfile = "$base.daa";
  $confile = "$base.conf";
}

print("Processing $infile\nWriting traffic file: $outfile\nWriting configuration file: $confile\n");
open(OUTFILE,">$outfile") || die "** Error: Cannot save file $outfile\n";
open(CONFILE,">$confile") || die "** Error: Cannot save file $confile\n";

my $header = 0;
my $do = -1;
my $doconf = 0;

my $current_time = -1;
my $new_time_step = 1;

my @onlyl = split(/,/,$only); # List of aircraft to be considered
my @butl = split(/,/,$but); # List of aircraft to be excluded
my @collabs = split(/,/,$labels); # New column names

my %indices; # Hash label -> index
my %colval;  # Hash label -> value
my %coluni;  # Hash label -> units

while (<INFILE>) {
  my $str = trim($_);
  foreach my $label (@collabs) {
    if ($str =~ /^$label\s*=\s*([^#]+)/) {
      my $val  = trim($1);
      my $unit = "unitless";
      if ($val =~ /^(.+)\s*\[(.+)\]/) {
	$val = trim($1);
	#if ($val < 5) {
	#  $val = 5;
	#}
	$unit = trim($2);
      }
      $colval{$label} = $val;
      $coluni{$label} = $unit;
    }
  }
  if (!$header) {
    if ($str =~ /NAME/ || $do == 0) {
      print OUTFILE "$str";
      if ($do < 0) {
	my @columns = split(/[ ,]+/,$str);
	my $idx = 0;
	foreach $column (@columns) {
	  if ($column =~ /NAME/) {
	    $indices{'NAME'} = $idx;
	  } elsif ($column =~ /time/) {
	    $indices{'time'} = $idx;  
	  }
	  ++$idx;
	}
	@collabs = keys(%colval);
	foreach my $label (@collabs) {
	  print OUTFILE " $label";
	}
      } else {
	foreach my $label (@collabs) {
	  print OUTFILE " [$coluni{$label}]";
	}
      }
      print OUTFILE "\n";
      $header = 1 if $do == 0;
      $do++;
    } elsif (/Daidalus Object/) {
      $doconf = 1;
    } elsif (/###/) {
      $doconf = 0;
    } elsif ($doconf) {
      print CONFILE "$str\n";
    }  
  } elsif (/NAME/ || $do == 0) {
    $do++;
    if (/NAME/) {
      $new_time_step = 1;
    }
  } elsif (!$str) {
    $do = -1;
  } elsif ($do > 0) {
    my @columns = split(/[ ,]+/,$str);
    my $nameidx = $indices{'NAME'};
    my $timeidx = $indices{'time'};
    if (!defined $columns[$nameidx] ||
	!defined $columns[$nameidx] ||
	!defined $columns[$timeidx]) {
      $do = -1;
      next;
    }
    next if ($but && grep(/$columns[$nameidx]/,@butl)) ||
      ($only && !grep(/$columns[$nameidx]/,@onlyl));
    my $t = $columns[$timeidx];
    if ($current_time == $t) {
      if ($new_time_step) {
	if ($fixtimes) {
	  $current_time++;
	  $columns[$timeidx] = $current_time;
	  $str = join(", ",@columns);
	  print "** Warning: $t --> $current_time\n";
	} else {
	  die "** Error: Time $t is the same a previous time. Try --fixtimes\n";
	}
      }
    } else {
      if (!$new_time_step) {
	if ($fixtimes) {
	  $columns[$timeidx] = $current_time;
	  $str = join(", ",@columns);
	  print "** Warning: $t --> $current_time\n";
	} else {
	  die "** Error: Time $t is different from current time\n";
	}
      } elsif ($t < $current_time) {
	if ($fixtimes) {
	  $current_time++;
	  $columns[$timeidx] = $current_time;
	  $str = join(", ",@columns);
	  print "** Warning: $t --> $current_time\n";
	} else {
	  die "** Warning: Time $t is less than previous time\n";
	}
      } else {
	$current_time = $t;
      }
    }
    print OUTFILE "$str";
    $new_time_step = 0;
    foreach my $label (@collabs) {
      print OUTFILE ", $colval{$label}";
    }
    print OUTFILE "\n";
  } 
}
close(INFILE);
close(OUTFILE);
close(CONFILE);


