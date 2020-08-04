#!/usr/bin/python
from util import units as u
from util import masterReadIn2 as r
from util import daa_funcs as f
import numpy as np
import matplotlib.pyplot as plt
import math
import argparse
from matplotlib.backends.backend_pdf import PdfPages
import os.path
import sys

#/
# This file produces different DAIDALUS graphs. 
# There are two important functions. labels_graphs produces the labels in the graphs, and plot_fun translates units and plots the function. 
# Several different types of graphs are produced by calling the functions. 
#/

# function to produce labels for graphs with metrics
def labels_graph(title,add_line):
	names = []
	for x in range(1,len(file.names)):
		names.append(title+'('+file.names[0]+','+file.names[x]+')')  
	if add_line != None:
		names.append(add_line)
	return names

# create parser 
parser = argparse.ArgumentParser(description = 'Produces graphs for detect and avoid (DAIDALUS)')
parser.add_argument('file', metavar = 'FILENAME', help = 'an input file')
parser.add_argument('--vs', help = 'generates vertical speed graph', action = 'store_true')
parser.add_argument('--gs', help = 'generates ground speed graph', action = 'store_true')
parser.add_argument('--alt', help = 'generates altitude graph', action = 'store_true')
parser.add_argument('--tcpa', help = 'generates tcpa graph', action = 'store_true')
parser.add_argument('--hmd', help = 'generates hmd graph', action = 'store_true')
parser.add_argument('--taumod', help = 'generates taumod graph', action = 'store_true')
parser.add_argument('--tcoa', help = 'generates tcoa graph', action = 'store_true')
parser.add_argument('--vmd', help = 'generates vmd graph', action = 'store_true')
parser.add_argument('--vd', help = 'generates vertical distance graph', action = 'store_true')
parser.add_argument('--hd', help = 'generates horizontal distance graph', action = 'store_true')
parser.add_argument('--trk', help = 'generates track angle graph', action = 'store_true')
parser.add_argument('--show', help = 'show figures', action = 'store_true')
parser.add_argument('--conf', metavar = 'CONFIGURATIONFILE', help = 'load CONFIGURATIONFILE', default = '')
args = parser.parse_args()
base = os.path.splitext(args.file)
basename = os.path.splitext(os.path.basename(args.file))[0]
config = args.conf
if not config:
	config = base[0] + '.conf'

# config to be read in with DAIDALUS units
units = {}
values = {}

# check to make sure that file exists
try:
	with open(args.file, 'rU') as file:
		print("Reading data file "+args.file)
except IOError:
	print('** Error: Data file ' + args.file + ' not found')
	sys.exit(1)

# Default values and units
values['DMOD']   = 0
values['TAUMOD'] = 0
values['ZTHR']   = 0
values['TCOA']   = 0
values['D']      = 0
values['H']      = 0
units['DMOD']    = 'm'
units['TAUMOD']  = 's'
units['ZTHR']    = 'm'
units['TCOA']    = 's'
units['D']       = 'm'
units['H']       = 'm'

# read in configuration file
try:
	with open(config, 'rU') as file:
		print('Reading configuration file '+config)
		for line in file:
			if not line or line.startswith("#"):
				continue
			else:
				columns = line.strip().replace('=', ' ').replace('[', '').replace(']', '').replace('\t', '').split();
				columns = [ y.strip() for y in columns ]
				try:
					values[columns[0]] = float(columns[1])
				except ValueError:
					values[columns[0]] = columns[1]
				try:
					units[columns[0]] = columns[2]
				except IndexError:
					units[columns[0]] = 'unitless'
except IOError:
	if args.conf:
		print('** Error: Configuration file '+args.conf+' not found')
		sys.exit(1)

# None: undef, 0: CD3D, 1: WCV
WCV = None        
# try to read in the conf file
try:
	det_var = values['alert_' + str(int(values['conflict_level'])) + '_detector']
	try:
		values['DMOD'] = values[det_var+'_WCV_DTHR']
		values['TAUMOD'] = values[det_var+'_WCV_TTHR']
		values['ZTHR'] = values[det_var+'_WCV_ZTHR']
		values['TCOA'] = values[det_var+'_WCV_TCOA']
		WCV = 1
		units['DMOD'] = units[det_var+'_WCV_DTHR']
		units['TAUMOD'] = units[det_var+'_WCV_TTHR']
		units['ZTHR'] = units[det_var+'_WCV_ZTHR']
		units['TCOA'] = units[det_var+'_WCV_TCOA']
	except KeyError:
		pass
	try:
		values['D'] = values[det_var+'_D']
		values['H'] = values[det_var+'_H']
		WCV = 0
		units['D'] = units[det_var+'_D']
		units['H'] = units[det_var+'_H']
	except KeyError:
		pass
except KeyError:
	pass
# filename of file to be read in with flight data
file = r.Info(args.file)
# store units from data from the masterReadin2.py file
time = file.time
position = file.position
velocity = file.velocity
sz = file.sz
vz = file.vz
vs = file.vs
gs = file.gs
trk = file.track_angle
altitude = file.altitude

## function to generate plots
# paramaters are x_plot (list of x component), y_plot (list of y component), boundaries (y limit if they're in the file), 
# add_line (for adding a line to some plots), title (title of y axis), labels (labels of lines), from_units (units of y_plot), 
# to_units (units for y label, if None then uses from_units)
def plot_fun(x_plot, y_plot, boundaries, add_line, title, labels, from_units, to_units):
	file = r.Info(args.file)
	if to_units != None:
		for x in range(len(y_plot)):
			y_plot[x] = [u.convert_string(from_units, to_units, y) for y in y_plot[x]]
	else:
		to_units = from_units
	y_min = None
	y_max = None

	# generate graphs
	for x in range(0,len(y_plot)):
		if y_min == None:
			y_min = min(y_plot[x])
		else:
			y_min = min(y_min,min(y_plot[x]))
		if y_max == None:
			y_max = max(y_plot[x])
		else:
			y_max = max(y_max,max(y_plot[x]))
		if len(x_plot) == 1:
			plt.plot(x_plot[0], y_plot[x])
		else:
			plt.plot(x_plot[x], y_plot[x])

	# add an extra line for a constant 
	if add_line != None:
		plt.axhline(y = values[add_line], color = 'm', linewidth=2.0)
		y_min = min(y_min,values[add_line])
		y_max = max(y_max,values[add_line])

	plt.title(title + ' vs. time', fontsize = 20)
	plt.ylabel(title +  ' [' + to_units + ']')
	plt.xlabel('time [' + file.units_list[0][file.times_place] + ']')
	plt.legend(labels, loc = "best")
	plt.grid()
	# limit y axis if it's in the file
	if boundaries != None and 'min_' + boundaries in values.keys() and 'max_' + boundaries in values.keys():
		y_min = values['min_' + boundaries]
		y_max = values['max_' + boundaries]
	delta = 0.1*(y_max - y_min)
	plt.ylim([y_min-delta,y_max+delta])
	# show or save the graph as a pdf 
	if args.show:
		plt.show()
	else:
		title = title.replace(' ', '_')
		title = basename + '_' + title + '.pdf'
		with PdfPages(title) as pdf:
			pdf.savefig(transparent = True)
			plt.clf()
			print('Writing PDF file ' + title)

# plot vertical speed - units given in file
if args.vs:
	plot_fun([time], vs, 'vs', None, 'vertical speed', file.names, file.units_list[0][file.vs_place], None)

# plot ground speed - units given in file
if args.gs:
	plot_fun([time], gs, 'gs', None, 'ground speed', file.names, file.units_list[0][file.gs_place], None)

# plot altitude - units given in file
if args.alt:
	plot_fun([time], altitude, 'alt', None, 'atltitude', file.names, file.units_list[0][file.alt_place], None)

# track angle - units from: given in file, units to: degrees
if args.trk:
	plot_fun([time], trk, None, None, 'track angle',file.names, file.units_list[0][file.trk_place], 'deg')

# plot tcpa - units come from and to: TAUMOD file units / no added line 
if args.tcpa:
	tcpa_print = [[] for x in range(len(position))]
	time_print = [[] for x in range(len(position))]
	# call tcpa function from daa_funcs.py
	for x in range(0, len(position)):
		for y in range(0, len(position[x])):
			temp = f.tcpa(position[x][y], velocity[x][y])
			# only plot values that are not zero 
			if temp != 0:
				tcpa_print[x].append(temp)
				time_print[x].append(time[y])
	plot_fun(time_print, tcpa_print, None, None, 'tcpa', labels_graph('tcpa', None), units['TAUMOD'], None)

# plot horizontal miss distance - from units: meters, to units: in DMOD / add DMOD line
if args.hmd:
	hmd_print = [[] for x in range(len(position))]
	time_print = [[] for x in range(len(position))]
	# call hmd function from daa_funcs.py
	for x in range(0, len(position)):
		for y in range(0, len(position[x])):
			temp = f.hmd(position[x][y], velocity[x][y])
			# only plot positive values
			if temp > 0:
				hmd_print[x].append(temp)
				time_print[x].append(time[y])
	if WCV == 0:
		plot_fun(time_print, hmd_print, None, 'D', 'hmd', labels_graph('hmd', 'D'), 'm', units["D"])
	else:
		plot_fun(time_print, hmd_print, None, 'DMOD', 'hmd', labels_graph('hmd', 'DMOD'), 'm', units["DMOD"])

# plot vertical miss distance - from units: altitude units, to units: in ZTHR / add ZTHR line
if args.vmd:
	vmd = [[] for x in range(len(sz))]
	# call vmd function from daa_funcs.py
	for x in range(0, len(sz)):
		for y in range(0, len(sz[x])):
			temp = f.vmd(sz[x][y], vz[x][y])
			vmd[x].append(temp)
	if WCV == 0:
		plot_fun([time], vmd, None, 'H', 'vmd', labels_graph('vmd', 'H'), file.units_list[0][file.alt_place], units['H'])
	else: 
		plot_fun([time], vmd, None, 'ZTHR', 'vmd', labels_graph('vmd', 'ZTHR'), file.units_list[0][file.alt_place], units['ZTHR'])

# plot taumod - from units: time in file, to units: in TAUMOD / add TAUMOD line
if args.taumod:
	taumod_print = [[] for x in range(len(position))]
	time_print = [[] for x in range(len(position))]
	# call taumod function from daa_funcs.py
	for x in range(0, len(position)):
		for y in range(0, len(position[x])):
			temp = f.taumod(position[x][y], velocity[x][y], values['DMOD'])
			# only plot positive values
			if temp > 0:
				taumod_print[x].append(temp)
				time_print[x].append(time[y])   
	plot_fun(time_print, taumod_print, None, 'TAUMOD', 'taumod', labels_graph('taumod', 'TAUMOD'), file.units_list[0][file.times_place], units['TAUMOD'])

# plot tcoa - from units: time in file, to units: in TCOA / add TCOA line 
if args.tcoa:
	tcoa_print = [[] for x in range(len(sz))]
	time_print = [[] for x in range(len(sz))]
	# call tcoa function from daa_funcs.py
	for x in range(0, len(sz)):
		for y in range(0, len(sz[x])):
			temp = f.tcoa(sz[x][y], vz[x][y])
			# only plot values that are not zero
			if temp != 0:
				tcoa_print[x].append(temp)
				time_print[x].append(time[y])	# remove any zero points from the data 			
	plot_fun(time_print, tcoa_print, None, 'TCOA', 'tcoa', labels_graph('tcoa', 'TCOA'),  file.units_list[0][file.times_place], units['TCOA'])

# horizontal distance - from units: meters (norm calculates meters) to units: in DMOD / add DMOD line
if args.hd:
	hd = [[] for x in range(len(position))]
	# calculate norm of position vector 
	for x in range(0, len(position)):
		for y in range(len(position[x])):
			temp = position[x][y].norm(position[x][y])
			# print(temp)
			hd[x].append(temp)
	if WCV == 0:
		plot_fun([time], hd, None, 'D', 'horizontal distance', labels_graph('hd', 'D'), 'm', units['D'])
	else:
		plot_fun([time], hd, None, 'DMOD', 'horizontal distance', labels_graph('hd', 'DMOD'), 'm', units['DMOD'])

# vertical distance - from units: units of altitude in file read in, to units: in ZTHR / add ZTHR line
if args.vd:
	vd = [[] for x in range(len(sz))]
	# calculate absolute values of each s_z point
	for x in range(0, len(sz)):
		for y in range(len(sz[x])):
			temp = abs(sz[x][y])
			vd[x].append(temp)
	if WCV == 0:
		plot_fun([time], vd, None, 'H', 'vertical distance', labels_graph('vd', 'H'), file.units_list[0][file.alt_place], units['H'])
	else:
		plot_fun([time], vd, None, 'ZTHR', 'vertical distance', labels_graph('vd', 'ZTHR'), file.units_list[0][file.alt_place], units['ZTHR'])

