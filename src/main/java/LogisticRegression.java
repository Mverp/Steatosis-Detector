

public class LogisticRegression
{
	final double[] factors;
	final double constant;


	public LogisticRegression(final double aConstant, final double[] aFactors)
	{
		this.factors = aFactors;
		this.constant = aConstant;
	}


	public double calculate(final double[] aValues)
	{
		double result = this.constant;
		for (int i = 0; i < this.factors.length; i++)
		{
			result += this.factors[i] * aValues[i];
		}
		result = Math.exp(-result) + 1;

		return 1 / result;
	}
}
