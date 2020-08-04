#!/usr/bin/python

#/
# This file reads in the files from the Formal ATM test files. It ignores any comments and blank spaces.
# If you want comments, you will have to open the file directly. This file only reads in 
# latidutde / longitude / altitude / time / ground velocity (if avaliable) / vertical velocity (if avaliable ).
# The file reads the data in as strings. It is converted to floats in the Flight_Info class
# The function also marks the position of where certain data are in the file
# The function does not return anything.
#/

import vectors as v
import vect3 as v3
import math
import GreatCircle as GC
import ENUprojection as ENU

class Info():
	def __init__(self, filename):
		self.filename = filename 
		self.spherical = True 
		self.spherical_velocity = False
		self.container = []
		self.data = []
		self.type_list = []
		self.names = []
		self.position = []
		self.velocity = []
		self.units_list = []
		self.vz = []
		self.sz = []
		self.time = []
		self.track_angle = []
		self.altitude = []
		self.lookahead_time = []
		self.gs = []
		self.vs = []
		self.trk_place = 0
		self.times_place = 0
		self.vs_place = 0
		self.vy_place = 0
		self.vz_place = 0
		self.gs_place = 0
		self.vs_place = 0
		self.alt_place = 0
		self.future_time_place = 0
		self.readIn()
		self.remove_incompletes(self.container)
		self.get_position_vectors()
		self.get_velocity_vectors()
		self.get_track_angle()
		self.get_sz()
		self.get_vz()
		self.get_time()
		self.get_gs()
		self.get_vs()
		self.get_altitude()
		self.get_future_time()


	def readIn(self):
		# open the file given by the user. This only opens the file in read mode. The file cannot be edited in this function
		line_count = 0
		count = 0
		with open(self.filename, "rU") as file:
			for line in file:
				line = line.strip()		
				# This loop reads in the line of data that indicates the data stored in the file
				if line.startswith('NAME'):
					self.type_list.append(line)
					# if there is an "x" in spherical that indicates that the data has coordinates in "sx, sy, sz," 
					# indicating Euclidian conversions need to be used
					if "x" in self.type_list[0][1]:
						self.spherical = False
					if "vx" in self.type_list[0]:
						self.spherical_velocity = True
					# The following three lines strip empty spaces and split the list
					self.type_list[0] = self.type_list[0].replace(',', ' ').replace('\t', ' ').replace(']', '').replace('[', '').split(" ")
					self.type_list[0] = [x.strip(' ') for x in self.type_list[0]]
					self.type_list[0] = filter(None, self.type_list[0])	
					# below there are markers that mark the place in the type_list[0] list where the columns for time and velocity are
					for x in range(0, len(self.type_list[0])):
						if self.type_list[0][x] == "time":
							self.times_place = x
					for x in range(0, len(self.type_list[0])):
						if self.type_list[0][x] == "gs":
							self.gs_place = x
						if self.type_list[0][x] == "vs":
							self.vs_place = x
						if self.type_list[0][x] == "vx":
							self.vx_place = x
						if self.type_list[0][x] == "vy":
							self.vy_place = x
						if self.type_list[0][x] == "vz":
							self.vz_place = x
						if self.type_list[0][x] == "time" or self.type_list[0][x] == "st":
							self.times_place = x
						if self.type_list[0][x] == "alt" or self.type_list[0][x] == "sz":
							self.alt_place = x
						if self.type_list[0][x] == "trk":
							self.trk_place = x	
						if self.type_list[0][x] == "lookahead_time":
							self.future_time_place = x	
					line_count += 1
					# set the starter bool equal to true to skip the first loop in the next iteration of the function
					continue
				# This is used later in Flight_Info and in plotting
				elif line.startswith(('unitless', '[none]')):
				# elif line_count == 1:
					line_count += 1
					self.units_list.append(line)
					# the next three lines replace any brackets or tabs in the line of unit types
					self.units_list[0] = self.units_list[0].replace(',', ' ').replace('\t', ' ').replace(']', '').replace('[', '').split(" ")
					self.units_list[0] = [x.strip(' ') for x in self.units_list[0]]
					self.units_list[0] = filter(None, self.units_list[0])
					continue
				# This loop reads in the pertinent data 
				elif line:
					# add the line to the list "container" with all the data
					self.container.append(line)
					# the next three lines are simply splitting up the row of data into a list without commas #thanksPython
					self.container[count] = self.container[count].replace(',', ' ').replace("\t", ' ').split(" ")
					self.container[count] = [x.strip(' ') for x in self.container[count]]
					self.container[count] = filter(None, self.container[count])
					# replace any ' " ' marker for names with the plane's name. This just makes it easier to convert data in the
					# Flight_Info class. This also makes it easier to find unique plane names
					self.container[count][0] = self.container[count][0].replace('"', self.container[count-1][0])
					# increment all the markers 
					name = self.container[count][0]
					if name not in self.names:
						self.names.append(name)
					count += 1
					line_count += 1


	def to_360(self, x):
		d = x * 180 / math.pi
		if d < 0:
			d += 360
		return d

	# this function removes any data from the ownship and intruder if all intruder planes 
	# do not have data for that time period
	def remove_incompletes(self, container):
		check = []
		size = len(self.names)

		if len(self.container) == size:
			for x in range(len(self.container)):
				self.data.append(self.container[x])

		for x in range(len(self.container)):
			if self.container[x][0] == self.container[0][0]:
				check.append(x)

		for x in range(len(check) - 1):
			temp = check[x+1] - check[x]
			if temp == size:
				for y in range(check[x], check[x+1]):
					self.data.append(container[y])


	# this function gets position vectors from the latitude and longitude, and converts them to
	# radians. If the data is not in xyz coordinates, it uses the functions in the ENUprojection.py file 
	# to turn them into xyz coordinates. Each list of vectors is stored in a 2D list called self.position
	def get_position_vectors(self):
		pso = []
		psi = [[] for x in range(len(self.names)-1)]
		si = [[] for x in range(len(self.names)-1)]
		self.position = [[] for x in range(len(self.names)-1)]
		origin = v.vector(0.0, 0.0)

		for y in range (0, len(self.data)):
			a = float(self.data[y][1])
			b = float(self.data[y][2])
			if not self.spherical:
				test = v.vector(a,b)
			else:
				a = math.radians(a)
				b = math.radians(b)
				test = ENU.spherical2xyz(a,b)
			# the next two loops put the temp vector in the appropriate slot
			if self.data[y][0] == self.names[0]:
				pso.append(test)
			else:
				for x in range(0, len(self.names)):
					if self.data[y][0] == self.names[x]:
						psi[x-1].append(test)
		
		# calculation using sphere_to_2D_plane 
		# if self.spherical:
		for x in range(0, len(psi)):
			for y in range(0, len(psi[x])):
				temp = ENU.sphere_to_2D_plane(pso[y], psi[x][y])
				si[x].append(temp)

		# calculate relative position 
		for x in range(0, len(psi)):
			for y in range(0, len(psi[x])):
				temp = origin - si[x][y]
				self.position[x].append(temp)

	
		# for word in self.position[0]:
		# 	print(word)

	# this function gets the ground speed from the file. it just reads the data directly in, 
	# and stores them in a 2D list called self.gs
	def get_gs(self):
		self.gs = [[] for x in range(len(self.names))]

		if self.spherical_velocity:
			for x in range(len(self.data)):
				temp_vector = v.vector(self.data[x][self.vx_place], self.data[x][self.vy_place])
				temp = temp_vector.norm(temp_vector)

				for y in range(len(self.names)):
					if self.data[x][0] == self.names[y]:
						self.gs[y].append(temp)

		else:
			for x in range(len(self.data)):
				temp = float(self.data[x][self.gs_place])

				for y in range(0, len(self.names)):
					if self.data[x][0] == self.names[y]:
						self.gs[y].append(temp)

	# this function gets the ground speed from the file. it just reads the data directly in, 
	# and stores them in a 2D list called self.gs
	def get_track_angle(self):
		self.track_angle = [[] for x in range(len(self.names))]

		if self.spherical_velocity:
			for x in range(len(self.data)):
				temp = math.atan2(float(self.data[x][self.vx_place]), float(self.data[x][self.vy_place]))
				trk = self.to_360(temp)

				for y in range(len(self.names)):
					if self.data[x][0] == self.names[y]:
						self.track_angle[y].append(trk)


		else:
			for x in range(len(self.data)):
				temp = float(self.data[x][self.trk_place])

				for y in range(len(self.names)):
					if self.data[x][0] == self.names[y]:
						self.track_angle[y].append(temp)

	# this function gets the vertical speed from the file. it just reads the data directly in, 
	# and stores them in a 2D list called self.vs
	def get_vs(self):
		self.vs = [[] for x in range(len(self.names))]

		if self.spherical_velocity:
			place = self.vz_place
		else:
			place = self.vs_place

		for x in range(len(self.data)):
			temp = float(self.data[x][place])

			for y in range(len(self.names)):
				if self.data[x][0] == self.names[y]:
					self.vs[y].append(temp)

	# this function gets the altitude from the file. it just reads the data directly in, 
	# and stores them in a 2D list called self.altitude
	def get_altitude(self):
		self.altitude = [[] for x in range(len(self.names))]
		for x in range(0, len(self.data)):
			temp = float(self.data[x][self.alt_place])

			for y in range(0, len(self.names)):
				if self.data[x][0] == self.names[y]:
					self.altitude[y].append(temp)

	# this function calculates the velocity vectors from the altitude and track angle, and then 
	# calculates the relative velocity and puts it in a 2D list called self.velocity
	def get_velocity_vectors(self):
		own_v = []
		intr_v = [[] for x in range(len(self.names)-1)]
		self.velocity = [[] for x in range(len(self.names)-1)]

		if self.spherical_velocity:
			for y in range(len(self.data)):
				test = v.vector(float(self.data[y][self.vx_place]), float(self.data[y][self.vy_place]))

				if self.data[y][0] == self.names[0]:
					own_v.append(test)
				else:
					for x in range(0, len(self.names)):
						if self.data[y][0] == self.names[x]:
							intr_v[x-1].append(test)



		else:
			for y in range (len(self.data)):
				temp_angle = float(self.data[y][self.trk_place])
				temp_gs = float(self.data[y][self.gs_place])

				if self.units_list[0][self.trk_place] == "deg":
					temp_angle = math.radians(temp_angle)

				test = v.vector(math.sin(temp_angle)*temp_gs, math.cos(temp_angle)*temp_gs)
		
				if self.data[y][0] == self.names[0]:
					own_v.append(test)
				else:
					for x in range(0, len(self.names)):
						if self.data[y][0] == self.names[x]:
							intr_v[x-1].append(test)

		for x in range(len(intr_v)):
			for y in range(0, len(intr_v[x])):
				temp = own_v[y] - intr_v[x][y]
				self.velocity[x].append(temp)


	# This file gets the relative altitude information directly from the file, and then calculates the 
	# relative altitude of ownship verses each intruder and stores the information in a 2D list called 
	# self.sz
	def get_sz(self):
		own_sz = []
		intr_sz = [[] for x in range(len(self.names)-1)]
		self.sz = [[] for x in range(len(self.names)-1)]

		for y in range (0, len(self.data)):
			a = float(self.data[y][self.alt_place])
			if self.data[y][0] == self.names[0]:
				own_sz.append(a)
			else:
				for x in range(0, len(self.names)):
					if self.data[y][0] == self.names[x]:
						intr_sz[x-1].append(a)

		for x in range(0, len(intr_sz)):
			for y in range(0, len(intr_sz[x])):
				temp = own_sz[y] - intr_sz[x][y]
				self.sz[x].append(temp)

	# This file gets the relative vertical velocity information directly from the file, and then calculates the 
	# relative altitude of ownship verses each intruder and stores the information in a 2D list called 
	# self.vz	
	def get_vz(self):
		own_vz = []
		intr_vz = [[] for x in range(len(self.names)-1)]
		self.vz = [[] for x in range(len(self.names)-1)]

		if self.spherical_velocity:
			place = self.vz_place
		else:
			place = self.vs_place

		for y in range (0, len(self.data)):
			a = float(self.data[y][place])
			if self.data[y][0] == self.names[0]:
				own_vz.append(a)
			else:
				for x in range(0, len(self.names)):
					if self.data[y][0] == self.names[x]:
						intr_vz[x-1].append(a)

		for x in range(0, len(intr_vz)):
			for y in range(0, len(intr_vz[x])):
				temp = own_vz[y] - intr_vz[x][y]
				self.vz[x].append(temp)

	# This file gets the time information directly from the file, but since time values are repeated for the 
	# ownship and the intruder, it just gets the time for the ownship and stores it in a 2D list called self.time
	def get_time(self):
		name = self.names[0]
		for x in range(0, len(self.data)):
			if self.data[x][0] == name:
				self.time.append(float(self.data[x][self.times_place]))


	# This file gets the future time information directly from the file, but since time values are repeated for the 
	# ownship and the intruder, it just gets the time for the ownship and stores it in a 2D list called self.time
	def get_future_time(self):
		name = self.names[0]
		# check to make sure that file has lookahead_time
		if self.future_time_place == 0:
			return

		for x in range(0, len(self.data)):
			if self.data[x][0] == name:
				self.lookahead_time.append(float(self.data[x][self.future_time_place]))


