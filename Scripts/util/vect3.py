#!/usr/bin/python

import math 
import vectors as v

#/ 
# This file makes a class to hold 3D vectors. Most basic vector operations are included, as well as overriding 
# addition, subtraction, division, multiplication, and priting. 
#/ 

class vector():
	def __init__(self, x = 0, y = 0, z = 0):
		self.x = x
		self.y = y
		self.z = z

	# define dot product 
	def dot(self, other1):
		return self.x*other1.x + self.y*other1.y + self.z*other1.z

	# define square 
	def sqv(self, other):
		return self.dot(other)

	# define norm 
	def norm(self, other):
		return math.sqrt(self.sqv(other))

	# define normalized 
	def normalized(self, other):
		return (other / self.norm(other))

	# define hat 
	def hat(self):
		n = norm(self)
		if n == (0.0):
			return self
		else:
			return vector(self.x/n, self.y/n, self.z/n)

	# define cross product of two vectors
	def cross(self, other2):
		return vector(self.y*other2.z - self.z*other2.y, self.z*other2.x - self.x*other2.z, self.x*other2.y - self.y*other2.x);

	# return a 2D vector comprised of x and y components of 2D vector
	def vect2(self):
		return v.vector(self.x, self.y)

	# define scaling 
	def scal(self, k):
		return self*k

	# override addition
	def __add__(self, other1):
		return vector(self.x + other1.x, self.y + other1.y, self+z + other1.z)

	# override subtraction
	def __sub__(self, other1):
		return vector(self.x - other1.x, self.y - other1.y, self.z - other1.z)

	# override division
	def __div__(self, val):
		return vector(self.x/val, self.y/val, self.z/val)

	# override multiplication 
	def __mul__(self, val):
		return vector(self.x*val, self.y*val, self.z*val)

	# override printing vector
	def __str__(self):
		return "(" + str(self.x) + ", " + str(self.y) +  ", " + str(self.z) + ")"

