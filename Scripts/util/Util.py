class Util():
	def __init__(self):
		self.twopi = 2*math.pi

	def to_pi(self, rad):
		r = to_2pi(rad)
		if f > math.pi:
			return r - self.twopi
		else:
			return r

	def two_2pi(self, rad):
		return rad%self.twopi
