#!/usr/bin/python

import math

#/ 
# This file makes a class to hold 2D vectors. Most basic vector operations are included, as well as overriding 
# addition, subtraction, division, multiplication, and priting. 
#/ 

class vector():
	def __init__(self, x = 0, y = 0):
		self.x = float(x)
		self.y = float(y)

	# override addition
	def __add__(self, val):
		return vector(self.x + val.x, self.y + val.y)

	# override subtraction
	def __sub__(self, val):
		return vector(self.x - val.x, self.y - val.y)

	# override division
	def __div__(self, val):
		return vector(self.x/val, self.y/val)

	# override multiplication
	def __mul__(self, val):
		return vector(self.x*val, self.y*val)

	# override printing
	def __str__(self):
		return "(" + str(self.x) + ", " + str(self.y) + ")"

	# define the dot product
	def dot(self, other1, other2):
		return other1.x*other2.x + other1.y*other2.y

	# define the norm
	def norm(self, other):
		return math.sqrt(self.sqv(other))

	# define square 
	def sqv(self, other):
		temp = other.x**2 + other.y**2
		return temp

	# define scalar multiplication 
	def scal(self, val, other):
		return other*val

	# define addition and scalar multiplication
	def addScal(self, val, other):
		return self+other.sca(val)

	def prepR(self):
		return vector(self.y, - self.x)

	def det(self,other):
		return self.dot(other.perpR())

	def ae(self, other):
		return (ae(self.x, other.x) and ae(self.y, other.y))

	def hat(self):
		if self.norm() == 0:
			return self
		else:
			return self.scal(1/self.norm())

	def topair(self):
		return (self.x, self.y)
