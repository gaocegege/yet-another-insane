object test {
	def aa() = {
		3
	}
	def a() {
		var x = 1
		if (x == 1) {
			a()
		}
		else {
			aa()
		}
	}

	def main(args: Array[String]) = {
		a()
	}
}