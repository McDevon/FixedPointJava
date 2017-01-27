package com.mcdevon.fixed;

import java.math.BigDecimal;

public final class Fixed {
	private final int _data;
	
	// Static values for 32-bit fixed-point value
	private static final int MAX_VALUE = Integer.MAX_VALUE;
	private static final int MIN_VALUE = Integer.MIN_VALUE;
	
	public static final int BITS = 32;
	
	// NOTE: Decimal bits must be less than or equal to (BITS / 2) for mul() to work
	// and an even number for sqrt() to work!
	private static final int DECIMAL_BITS = 10;
	private static final int DECIMAL_MASK = new Integer(-1) >>> (BITS - DECIMAL_BITS);
	private static final int FULL_MASK = new Integer(-1);
	
	private static final int ONE = 1 << DECIMAL_BITS;
	private static final int TWO = 1 << (DECIMAL_BITS + 1);
	private static final int HALF = 1 << (DECIMAL_BITS - 1);
	
	public static final Fixed maxValue = new Fixed(MAX_VALUE);
	public static final Fixed minValue = new Fixed(MIN_VALUE);
	public static final Fixed zero = new Fixed(0);
	public static final Fixed one = new Fixed(ONE);
	public static final Fixed two = new Fixed(TWO);
	public static final Fixed half = new Fixed(HALF);
	public static final Fixed precision = new Fixed(1);
	
	// String part lengths
	//private static final int _leftLength = Integer.toString(maxValue.intValue()).length();
	private static final int _rightLength;
	static {
		String[] parts = precision.bigDecimalValue().toString().split("\\.");
		if (parts.length <= 1) {
			_rightLength = 0;
		} else {
			int res = 1;
			for (int i = 0; i < parts[1].length(); i++) {
				if (parts[1].charAt(i) == '0') {
					res++;
					continue;
				}
				break;
			}
			_rightLength = res;
		}
	}
	
	private static Fixed[] _tenPowerTable = new Fixed[] {
			Fixed.fromLong(1L),  // 10^0
			Fixed.fromLong(10L),  // 10^1
			Fixed.fromLong(100L),  // 10^2
			Fixed.fromLong(1000L),  // 10^3
			Fixed.fromLong(10000L),  // 10^4
			Fixed.fromLong(100000L),  // 10^5
			Fixed.fromLong(1000000L),  // 10^6
			Fixed.fromLong(10000000L),  // 10^7
			Fixed.fromLong(100000000L),  // 10^8
			Fixed.fromLong(1000000000L),  // 10^9
			Fixed.fromLong(10000000000L),  // 10^10
			Fixed.fromLong(100000000000L),  // 10^11
			Fixed.fromLong(1000000000000L),  // 10^12
			Fixed.fromLong(10000000000000L),  // 10^13
			Fixed.fromLong(100000000000000L),  // 10^14
			Fixed.fromLong(1000000000000000L),  // 10^15
			Fixed.fromLong(10000000000000000L),  // 10^16
			Fixed.fromLong(100000000000000000L),  // 10^17
			Fixed.fromLong(1000000000000000000L),  // 10^18
	};
	
	public static final Fixed pi = Fixed.fromString("3.14159265358979323846264338327950288419716939937510");
	public static final Fixed piTimesTwo = Fixed.fromString("6.28318530717958647692528676655900576839433879875020");
	public static final Fixed piOverTwo = Fixed.fromString("1.57079632679489661923132169163975144209858469968755");
	public static final Fixed piInv = Fixed.fromString("0.31830988618379067153776752674502872406891929148091");
	public static final Fixed piOverTwoInv = Fixed.fromString("0.63661977236758134307553505349005744813783858296183");
		
	// Values for sin/tan look-up table generation and use
	private static final int LUT_LERP_LIMIT = 14;
	private static final boolean LUT_LERP_IN_USE = DECIMAL_BITS > LUT_LERP_LIMIT;
	private static final int LUT_SIZE = LUT_LERP_IN_USE ? Fixed.fromInt((1 << LUT_LERP_LIMIT)).mul(piOverTwo).intValue() : piOverTwo._data;
	private static final Fixed LUT_INTERVAL = piOverTwo.safeDiv(Fixed.fromInt(LUT_SIZE - 1));
	private static final Fixed LUT_INTERVAL_INV = Fixed.fromInt(LUT_SIZE - 1).safeDiv(piOverTwo);
	
	private static final Fixed degToRad = pi.div(Fixed.fromInt(180));
	private static final Fixed radToDeg = Fixed.fromInt(180).safeDiv(pi);
	
	private static final int PI = pi._data;
	private static final int PI_TIMES_TWO = piTimesTwo._data;
	private static final int PI_OVER_TWO = piOverTwo._data;
	
	public static int lutSize() {
		return LUT_SIZE;
	}
	
	private Fixed(int data) {
		_data = data;
	}
	
	public static int sign(Fixed value) {
		return value._data < 0 ? -1 :
			value._data > 0 ? 1 : 0;
	}
	
	public static Fixed toRadians(Fixed value) {
		return value.mul(degToRad);
	}
	
	public static Fixed toDegrees(Fixed value) {
		return value.mul(radToDeg);
	}
	
	public static Fixed abs(Fixed value) {
		int mask = value._data >> (BITS - 1);
		return new Fixed((value._data + mask) ^ mask);
	}
	
	public static Fixed safeAbs(Fixed value) {
        if (value._data == MIN_VALUE) {
            return maxValue;
        }
		
		int mask = value._data >> (BITS - 1);
		return new Fixed((value._data + mask) ^ mask);
	}
	
	 public static Fixed floor(Fixed value) {
         // Just zero out the decimal part
         return new Fixed(value._data & (~DECIMAL_MASK));
     }
	 
     public static Fixed Ceiling(Fixed value) {
         boolean hasFrac = (value._data & DECIMAL_MASK) != 0;
         return hasFrac ? floor(value).add(one) : value;
     }
	
	public static Fixed round(Fixed value) {
		int fract = value._data & DECIMAL_MASK;
        Fixed integral = floor(value);
        if (fract < HALF) {
            return integral;
        }
        // Halves are always rounded upwards
        return integral.add(one);
	}
	
	public static Fixed roundRuleEven(Fixed value) {
		int fract = value._data & DECIMAL_MASK;
        Fixed integral = floor(value);
        if (fract < HALF) {
            return integral;
        }
        if (fract > HALF) {
        	return integral.add(one);
        }

        // Halves are rounded to nearest even number
        return (integral._data & ONE) == 0
                ? integral
                : integral.add(one);
	}
	
	/*
	 * Operators
	 */
	
	public Fixed add(Fixed value) {
		return new Fixed(_data + value._data);
	}
	
	public Fixed sub(Fixed value) {
		return new Fixed(_data - value._data);
	}
	
	public Fixed mul(Fixed value) {
        int xl = _data;
        int yl = value._data;

        int xlo = xl & DECIMAL_MASK;
        int xhi = xl >> DECIMAL_BITS;
        int ylo = yl & DECIMAL_MASK;
        int yhi = yl >> DECIMAL_BITS;

        int lolo = xlo * ylo;
        int lohi = xlo * yhi;
        int hilo = xhi * ylo;
        int hihi = xhi * yhi;

        int loResult = lolo >>> DECIMAL_BITS;
        int midResult1 = lohi;
        int midResult2 = hilo;
        int hiResult = hihi << DECIMAL_BITS;

        int sum = loResult + midResult1 + midResult2 + hiResult;
        return new Fixed(sum);
    }
	
	private static final int oneBitHighMask = MIN_VALUE;
    private static final int fourBitHighMask = oneBitHighMask >> 3;
	
	private static int leadingZeroes(int x) {
        int result = 0;
        while ((x & fourBitHighMask) == 0) { result += 4; x <<= 4; }
        while ((x & oneBitHighMask) == 0) { result += 1; x <<= 1; }
        return result;
    }

    public Fixed div(Fixed value) {
        int xl = _data;
        int yl = value._data;

        if (yl == 0) {
            throw new ArithmeticException("Divide by zero");
        }

        int remainder = xl >= 0 ? xl : (-xl) & (~MIN_VALUE);
        int divider = yl >= 0 ? yl : (-yl) & (~MIN_VALUE);
        int quotient = 0;
        int bitPos = DECIMAL_BITS + 1;

        // If the divider is divisible by 2^n, use bit shifts for faster calculation
        while ((divider & 0xF) == 0 && bitPos >= 4) {
            divider >>>= 4;
            bitPos -= 4;
        }

        while (remainder != 0 && bitPos >= 0) {
            int shift = leadingZeroes(remainder);
            if (shift > bitPos) {
                shift = bitPos;
            }
            remainder <<= shift;
            bitPos -= shift;

            int div = remainder / divider;
            remainder = remainder % divider;
            quotient += div << bitPos;

            // Detect overflow
            if ((div & ~(FULL_MASK >>> bitPos)) != 0) {
                return ((xl ^ yl) & MIN_VALUE) == 0 ? maxValue : minValue;
            }

            remainder <<= 1;
            bitPos--;
        }

        // rounding
        quotient++;
        int result = quotient >>> 1;
        if (((xl ^ yl) & MIN_VALUE) != 0) {
            result = -result;
        }

        return new Fixed(result);
    }
    
	public Fixed mod(Fixed value) {
		return new Fixed(_data % value._data);
	}
	
	public Fixed negate() {
		return new Fixed(-_data);
	}
	
	public boolean lessThan(Fixed value) {
		return _data < value._data;
	}
	
	public boolean greaterThan(Fixed value) {
		return _data > value._data;
	}
	
	public boolean lessThanOrEquals(Fixed value) {
		return _data <= value._data;
	}
	
	public boolean greaterThanOrEquals(Fixed value) {
		return _data >= value._data;
	}
	
	public boolean equals(Fixed value) {
		return _data == value._data;
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (obj == null) {
	        return false;
	    }
	    if (!(obj instanceof Fixed)) {
	        return false;
	    }
	    final Fixed other = (Fixed) obj;
	    return _data == other._data;
	}
	
	@Override
	public int hashCode() {
	    return _data;
	}
	
	/*
	 * Safe operators
	 */

	public Fixed safeAdd(Fixed value) {
        int xl = _data;
        int yl = value._data;
        int sum = xl + yl;
         
        // Check for overflows
        // TODO: Throw exception for overflow?
        if (((~(xl ^ yl) & (xl ^ sum)) & MIN_VALUE) != 0) {
            sum = xl > 0 ? MAX_VALUE : MIN_VALUE;
        }
        return new Fixed(sum);
	}
	
	public Fixed safeSub(Fixed value) {
        int xl = _data;
        int yl = value._data;
        int sub = xl - yl;
        
        // Check for overflows
        // TODO: Throw exception for overflow?
        if ((((xl ^ yl) & (xl ^ sub)) & MIN_VALUE) != 0) {
        	sub = xl < 0 ? MIN_VALUE : MAX_VALUE;
        }
        return new Fixed(sub);
	}
	
	public Fixed safeMul(Fixed value) {
        int xl = _data;
        int yl = value._data;

        int xlo = xl & DECIMAL_MASK;
        int xhi = xl >> DECIMAL_BITS;
        int ylo = yl & DECIMAL_MASK;
        int yhi = yl >> DECIMAL_BITS;

        int lolo = xlo * ylo;
        int lohi = xlo * yhi;
        int hilo = xhi * ylo;
        int hihi = xhi * yhi;

        int loResult = lolo >>> DECIMAL_BITS;
        int midResult1 = lohi;
        int midResult2 = hilo;
        int hiResult = hihi << DECIMAL_BITS;

        boolean overflow = false;
        
        int sum = loResult + midResult1;
        
        // a + b overflows if sign(x) ^ sign(y) != sign(sum),
        // test after every add
        overflow |= ((loResult ^ midResult1 ^ sum) & MIN_VALUE) != 0;
        
        int sumTest = sum + midResult2;
        overflow |= ((sum ^ midResult2 ^ sumTest) & MIN_VALUE) != 0;
        
        sum = sumTest + hiResult;
        overflow |= ((sumTest ^ hiResult ^ sum) & MIN_VALUE) != 0;

        boolean opSignsEqual = ((xl ^ yl) & MIN_VALUE) == 0;

        // check for same sign operands and negative result and for
        // different sign operands and positive result
        if (opSignsEqual) {
            if (sum < 0 || (overflow && xl > 0)) {
                return maxValue;
            }
        }
        else if (sum > 0) {
            return minValue;
        }

        // if the top bits of hihi are neither all 0s or 1s,
        // then this means the result overflowed.
        int topBits = hihi >> DECIMAL_BITS;
        if (topBits != 0 && topBits != -1) {
            return opSignsEqual ? maxValue : minValue; 
        }

        // Last case of negative overflow
        if (!opSignsEqual) {
            int posOp;
            int negOp;
            if (xl > yl) {
                posOp = xl;
                negOp = yl;
            }
            else {
                posOp = yl;
                negOp = xl;
            }
            if (sum > negOp && negOp < -ONE && posOp > ONE) {
                return minValue;
            }
        }

        return new Fixed(sum);
	}
	
    public Fixed safeDiv(Fixed value) {
        int xl = _data;
        int yl = value._data;

        if (yl == 0) {
            throw new ArithmeticException("Divide by zero");
        }

        int remainder = xl >= 0 ? xl : (-xl) & (~MIN_VALUE);
        int divider = yl >= 0 ? yl : (-yl) & (~MIN_VALUE);
        int quotient = 0;
        int bitPos = DECIMAL_BITS + 1;

        // If the divider is divisible by 2^n, use bit shifts for faster calculation
        while ((divider & 0xF) == 0 && bitPos >= 4) {
            divider >>>= 4;
            bitPos -= 4;
        }

        while (remainder != 0 && bitPos >= 0) {
            int shift = leadingZeroes(remainder);
            if (shift > bitPos) {
                shift = bitPos;
            }
            remainder <<= shift;
            bitPos -= shift;

            // Safer div and mod by casting to long
            int div = (int) ((remainder & 0xFFFFFFFFL) / (divider & 0xFFFFFFFFL));
            remainder = (int) ((remainder & 0xFFFFFFFFL) % (divider & 0xFFFFFFFFL));
            quotient += div << bitPos;

            // Detect overflow
            if ((div & ~(FULL_MASK >>> bitPos)) != 0) {
                return ((xl ^ yl) & MIN_VALUE) == 0 ? maxValue : minValue;
            }

            remainder <<= 1;
            bitPos--;
        }

        // rounding
        quotient++;
        int result = quotient >>> 1;
        if (((xl ^ yl) & MIN_VALUE) != 0) {
            result = -result;
        }

        return new Fixed(result);
    }

	
	public Fixed safeMod(Fixed value) {
		return new Fixed(_data == MIN_VALUE & value._data == -1 ? 0 :
                		 _data % value._data);
	}
	
	/*
	 * Math operations
	 */
		
	public static Fixed sqrt(Fixed x) {
		// BitShift-based sqrt
		
        int xl = x._data;
        if (xl < 0) {
            // Sqrt not defined for negative numbers and NaN not available
            throw new ArithmeticException("Sqrt for negative number");
        }

        int num = xl;
        int result = 0;

        // second-highest bit
        int bit = 1 << (BITS - 2);

        while (bit > num) {
            bit >>>= 2;
        }

        for (int i = 0; i < 2; i++) {
            // First we get the top bits of the answer.
            while (bit != 0) {
                if (num >= result + bit) {
                    num -= result + bit;
                    result = (result >>> 1) + bit;
                }
                else {
                    result = result >>> 1;
                }
                bit >>>= 2;
            }

            if (i == 0) {
                // Then process it again to get the lowest 16 bits.
                if (num > (1 << (DECIMAL_BITS)) - 1) {
                    // The remainder 'num' is too large to be shifted left
                    // by 32, so we have to add 1 to result manually and
                    // adjust 'num' accordingly.
                    // num = a - (result + 0.5)^2
                    //       = num + result^2 - (result + 0.5)^2
                    //       = num - result - 0.5
                    num -= result;
                    num = (num << (DECIMAL_BITS)) - HALF;
                    result = (result << (DECIMAL_BITS)) + HALF;
                }
                else {
                    num <<= (DECIMAL_BITS);
                    result <<= (DECIMAL_BITS);
                }

                bit = 1 << (DECIMAL_BITS - 2);
            }
        }
        // Finally, if next bit would have been 1, round the result upwards.
        if (num > result) {
            ++result;
        }
        return new Fixed(result);
    }
	
	public static Fixed sin(Fixed value) {
		int angle = value._data;
		
		// Clamp to 0...2pi
		int clamp2pi = angle % PI_TIMES_TWO;
		if (angle < 0) {
			clamp2pi += PI_TIMES_TWO;
        }
		
		// Clamp further to take use of luts
		boolean flipV = clamp2pi >= PI;
		int clampPi = clamp2pi;
		
		while (clampPi >= PI) {
            clampPi -= PI;
        }
		
		// Last clamp
        boolean flipH = clampPi >= PI_OVER_TWO;
        
        int clampPiPer2 = clampPi;
        if (clampPiPer2 >= PI_OVER_TWO) {
            clampPiPer2 -= PI_OVER_TWO;
		}
                
        if (LUT_LERP_IN_USE) {
        	// Use linear interpolation to get a bit more accurate value for sin
        	Fixed clamped = new Fixed(clampPiPer2);

        	// Find the two closest values in the lut
        	Fixed rawIndex = clamped.safeMul(LUT_INTERVAL_INV);
        	Fixed roundedIndex = roundRuleEven(rawIndex); 
        	Fixed indexError = rawIndex.sub(roundedIndex);

        	// Get the nearest values
        	// TODO: Fix luts to give out more accurate result
        	int index1 = flipH ? Math.abs(FixedPoint32Lut.sin.length - 1 - roundedIndex.intValue()): 
        				roundedIndex.intValue();
        	int index2 = flipH ? Math.abs(FixedPoint32Lut.sin.length - 1 - roundedIndex.intValue() - sign(indexError)) : 
        		roundedIndex.intValue() + sign(indexError);
        	
        	Fixed nearestValue = new Fixed(FixedPoint32Lut.sin[index1]);
        	Fixed secondNearestValue = new Fixed(FixedPoint32Lut.sin[index2]);

        	// Lerp to get final value
        	int delta = indexError.mul(abs(nearestValue.sub(secondNearestValue)))._data;
        	int interpolatedValue = nearestValue._data + (flipH ? -delta : delta);
        	int finalValue = flipV ? -interpolatedValue : interpolatedValue;
        	
        	return new Fixed(finalValue);
        } else {
            
        	// Expect to find most accurate value directly from lut
        	if (clampPiPer2 >= LUT_SIZE) {
        		clampPiPer2 = LUT_SIZE - 1;
        	}
        	
        	int result = FixedPoint32Lut.sin[flipH ? LUT_SIZE - 1 - clampPiPer2 :
        		clampPiPer2];
        	return new Fixed(flipV ? -result : result);
        }
	}
	
	public static Fixed cos(Fixed value) {
        int vd = value._data;
        int sinAngle = vd + (vd > 0 ? -PI - PI_OVER_TWO : PI_OVER_TWO);
        return sin(new Fixed(sinAngle));
    }
	
	public static Fixed tan(Fixed value) {
        int clampPi = value._data % PI;
        boolean flip = false;
        if (clampPi < 0) {
            clampPi = -clampPi;
            flip = true;
        }
        
        // Clamp to pi/2
        if (clampPi > PI_OVER_TWO) {
            flip = !flip;
            clampPi = PI_OVER_TWO - (clampPi - PI_OVER_TWO);
        }

        if (LUT_LERP_IN_USE) {
        	// Use linear interpolation to get a bit more accurate value for sin
        	Fixed clamped = new Fixed(clampPi);

        	// Find the two closest values in the lut
        	Fixed rawIndex = clamped.safeMul(LUT_INTERVAL_INV);
        	Fixed roundedIndex = roundRuleEven(rawIndex); 
        	Fixed indexError = rawIndex.sub(roundedIndex);

        	// Get the nearest values        	
        	Fixed nearestValue = new Fixed(FixedPoint32Lut.tan[roundedIndex.intValue()]);
        	Fixed secondNearestValue = new Fixed(FixedPoint32Lut.tan[roundedIndex.intValue() + sign(indexError)]);

        	// Lerp to get final value
        	int delta = indexError.mul(abs(nearestValue.sub(secondNearestValue)))._data;
        	int interpolatedValue = nearestValue._data + delta;
        	int finalValue = flip ? -interpolatedValue : interpolatedValue;
        	
        	return new Fixed(finalValue);
        } else {
            
        	// Expect to find most accurate value directly from lut
        	if (clampPi >= LUT_SIZE) {
        		clampPi = LUT_SIZE - 1;
        	}
        	
        	int result = FixedPoint32Lut.tan[clampPi];
        	return new Fixed(flip ? -result : result);
        }
    }
	
	private static Fixed atan2Help = Fixed.fromString("0.28");

    public static Fixed atan2(Fixed y, Fixed x) {
		// Approximate atan2 with error < 0.005 (if enough decimal bits)
        int yl = y._data;
        int xl = x._data;
        
        // div by zero cases
        if (xl == 0) {
            if (yl > 0) {
                return piOverTwo;
            }
            if (yl == 0) {
                return zero;
            }
            return piOverTwo.negate();
        }
        Fixed atan;
        Fixed z = y.div(x);

        Fixed divider = one.add(atan2Help.safeMul(z).safeMul(z));
        
        // overflow check
        if (divider.equals(maxValue)) {
            return y.lessThan(zero) ? piOverTwo.negate() : piOverTwo;
        }

        if (abs(z).lessThan(one)) {
            atan = z.div(divider);
            if (xl < 0) {
                if (yl < 0) {
                    return atan.sub(pi);
                }
                return atan.add(pi);
            }
        }
        else {
            atan = piOverTwo.sub(z.div(z.safeMul(z).add(atan2Help)));
            if (yl < 0) {
                return atan.sub(pi);
            }
        }
        return atan;
    }
	
	/*
	 * Look-up table generation
	 */
	
	static void generateLutFile() {
		
		try (java.io.PrintWriter writer = new java.io.PrintWriter("src/com/mcdevon/fixed/FixedPoint32Lut.java", "UTF-8")) {
			// Header
		    writer.println("package com.mcdevon.fixed;\n");
		    writer.println("public class FixedPoint32Lut {");
		    
		    // Sin lut
		    writer.println("    public static int[] sin = {");
		    writer.print("       ");
		    
		    int k = 1;
            for (int i = 0; i < LUT_SIZE; ++i) {
                double angle = new Fixed(i * LUT_INTERVAL._data).doubleValue(); // Math.PI * 0.5 / (LUT_SIZE - 1);
                if (k % 8 == 0) {
                	writer.print("\n       ");
                }
                k++;
                double sin = Math.sin(angle);
                Fixed val = Fixed.fromDouble(sin);
                writer.print(String.format(" %d,", val._data));
            }
            
            // Tan lut
            writer.println("\n    };\n");
            writer.println("    public static int[] tan = {");
		    writer.print("       ");
		    
		    k = 1;
		    boolean overflow = false;
            for (int i = 0; i < LUT_SIZE; ++i) {
    			double angle = new Fixed(i * LUT_INTERVAL._data).doubleValue();
                if (k % 8 == 0) {
                	writer.print("\n       ");
                }
                k++;
                double tan = Math.tan(angle);
                Fixed result;
                // After first overflow, all remaining values are maxValue
                if (overflow || tan > maxValue.doubleValue() || tan < 0.0) {
                	result = maxValue;
                	if (!overflow) {
                		overflow = true;
                	}
                } else {
                	result = Fixed.fromDouble(tan);
                }
                writer.print(String.format(" %d,", result._data));
            }

            // Footer
		    writer.println("\n    };\n}");
		    writer.close();
		} catch (java.io.IOException e) {
		   System.err.println(e.getLocalizedMessage());
		}
	}	
	static void generateDynamicLutData() {
		
		FixedPoint32Lut.sin = new int[LUT_SIZE];
		FixedPoint32Lut.tan = new int[LUT_SIZE];
		
		for (int i = 0; i < LUT_SIZE; ++i) {
			Fixed startValue = new Fixed(i * LUT_INTERVAL._data);
			double angle = startValue.doubleValue();
			double sin = Math.sin(angle);
			Fixed result = Fixed.fromDouble(sin);
			FixedPoint32Lut.sin[i] = result._data;
		}

		boolean overflow = false;
		for (int i = 0; i < LUT_SIZE; ++i) {
			double angle = new Fixed(i * LUT_INTERVAL._data).doubleValue();
			double tan = Math.tan(angle);
			Fixed result;
			// After first overflow, all remaining values are maxValue
			if (overflow || tan > maxValue.doubleValue() || tan < 0.0) {
				result = maxValue;
				if (!overflow) {
					overflow = true;
				}
			} else {
				result = Fixed.fromDouble(tan);
			}
			FixedPoint32Lut.tan[i] = result._data;
		}
	}
	
	/*
	 * Compatibility
	 */
	
	public static Fixed fromData(int dataValue) {
		return new Fixed(dataValue);
	}
	
	public static Fixed fromInt(int value) {
		return new Fixed(value * ONE);
	}
	
	public static Fixed fromLong(long value) {
		return new Fixed((int)value * ONE);
	}
	
	public static Fixed fromFloat(float value) {
		return new Fixed((int)(value * ONE));
	}
	
	public static Fixed fromDouble(double value) {
		return new Fixed((int)(value * ONE));
	}
	
	public static Fixed fromString(String stringValue) {
		String[] parts = null;
		String[] delimiters = {"\\.",","};

		for (int i = 0; i < delimiters.length && (parts == null || parts.length == 1); i++) {
			parts = stringValue.split(delimiters[i]);
		}

		if (parts.length > 2 || parts.length == 0) {
			throw new NumberFormatException("Invalid input string");
		}

		int left = Integer.parseInt(parts[0]);

		if (left > maxValue.intValue()) {
			throw new NumberFormatException("Invalid input string: too large number");
		}
		
		if (parts.length == 2) {

			if (parts[1].length() > _rightLength) {
				parts[1] = parts[1].substring(0, _rightLength);
			}
			
			int right = Integer.parseInt(parts[1]);
			Fixed divider = _tenPowerTable[parts[1].length()]; // 10 ^parts.Length
			
			// Decimal precision of fromString may decrease when DECIMAL_BITS == 16
			while (divider.intValue() < 0) {
				parts[1] = parts[1].substring(0, parts[1].length() - 1);
				right = Integer.parseInt(parts[1]);
				divider = _tenPowerTable[parts[1].length()];
			}
				
			Fixed fLeft = Fixed.fromInt(left);
			Fixed sign = fLeft.lessThan(Fixed.zero) ? Fixed.one.negate() : Fixed.one;

			Fixed lr = Fixed.fromInt(right);
			lr = lr.safeDiv(divider);
			lr = lr.mul(sign);
			
			return fLeft.add(lr);
		}

		return Fixed.fromInt(left);
	}
	
	public float floatValue() {
        return (float)_data / ONE;
	}
	
	public double doubleValue() {
        return (double)_data / ONE;
	}
	
	public int intValue() {
		return _data >> DECIMAL_BITS;
	}
	
	public BigDecimal bigDecimalValue() {
		return new BigDecimal(_data).divide(new BigDecimal(ONE));
	}
	
	public int dataValue() {
		return _data;
	}
	
	public String toString() {
		return bigDecimalValue().toString();
	}
	
	public static String info() {
		StringBuilder builder = new StringBuilder();
		builder.append("Precision: ");
		builder.append(precision);
		builder.append("\n");
		
		builder.append("Max value: ");
		builder.append(maxValue);
		builder.append("\n");
		
		builder.append("Min Value: ");
		builder.append(minValue);
		builder.append("\n");
		
		return builder.toString();
	}
	
	/*public static void main (String args[]) {
	 	// Do not use for DECIMAL_BITS > 10, lut file generation must be fixed for larger luts
	 	// Use dynamic lut generation instead
		generateLutFile();
	}*/
}
