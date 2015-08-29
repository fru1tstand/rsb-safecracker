package me.fru1t.rsbot.common.strategies.logic;

import org.powerbot.script.ClientContext;

import me.fru1t.common.annotations.Inject;
import me.fru1t.common.annotations.Nullable;
import me.fru1t.common.annotations.Singleton;
import me.fru1t.common.collections.Tuple2;
import me.fru1t.rsbot.common.framework.components.Persona;
import me.fru1t.rsbot.common.util.Random;

/**
 * The most impatient people will click more than 1 time. Someone sporadically
 * clicking will not know how many times they've clicked (or care to click a
 * consistent amount every time). The delay in clicks tends toward a unimodal
 * symmetric normal distribution (n = 300). However, because people are
 * different, the mean and variance of these curve are too.
 *
 * <p> Consider: Someone may become impatient, or fall out of impatience. Also, as
 * time wears on, fatigue may build up reducing both click count and click delay
 * mean.
 */
// TODO: Convert to singleton util.
public class SpamClick {
	/**
	 * Interface that defines an action to spam click.
	 */
	public static interface Action {
		public void interact();
	}

	/**
	 * Provides instances of SpamClick.
	 */
	public static class Factory {
		private final Persona persona;
		private final ClientContext<?> ctx;

		@Inject
		public Factory(
				@Singleton Persona persona,
				@Singleton ClientContext<?> ctx) {
			this.ctx = ctx;
			this.persona = persona;
		}

		/**
		 * Creates a new instance of SpamClick with the given parameters.
		 *
		 * @param delayIsRandomProbability The probability that the delay is random instead of
		 * normally distributed.
		 * @param clickCountIsRandomProbability The probability that the click count is random
		 * instead of normally distributed.
		 * @param varianceIsFocusDependentProbability The probability that the variance given to
		 * the gaussian function is focus dependent versus random. Nullable if both
		 * delayIsRandomProbability and clickCountIsRandomProbability are both 100.
		 * @param clickCountMeanRange The range of means the spam click instance should be able to
		 * return.
		 * @param clickCountVarianceRange The range of variance for distribution of click counts to be
		 * created with. This can be nulled if the clickCountIsRandomProbability is 100.
		 * @param delayMeanRange The range of means the delay between clicks should be.
		 * @param delayVarianceRange The range of variances the delay between clicks should be. This
		 * can be nulled if the delayCountIsRandomProbability is 100.
		 * @return A new SpamClick instance with the given parameters.
		 */
		public SpamClick create(
				int clickCountIsRandomProbability,
				int delayIsRandomProbability,
				@Nullable Integer varianceIsFocusDependentProbability,
				Tuple2<Integer, Integer> clickCountMeanRange,
				@Nullable Tuple2<Double, Double> clickCountVarianceRange,
				Tuple2<Integer, Integer> delayMeanRange,
				@Nullable Tuple2<Double, Double> delayVarianceRange) {
			if (varianceIsFocusDependentProbability == null
					&& (delayIsRandomProbability < 100 || clickCountIsRandomProbability < 100)) {
				throw new RuntimeException("The probability that variance is focus dependent "
						+ "must be given if delay or click count randomness is not guaranteed.");
			}
			if (clickCountVarianceRange == null && clickCountIsRandomProbability < 100) {
				throw new RuntimeException("Click count variance must be defined if click count "
						+ "is not guaranteed to be random.");
			}
			if (delayVarianceRange == null && delayIsRandomProbability < 100) {
				throw new RuntimeException("Delay variance must be defined if variance is not "
						+ "gauranteed to be random.");
			}
			return new SpamClick(
					ctx,
					persona,
					Random.roll(delayIsRandomProbability),
					Random.roll(clickCountIsRandomProbability),
					Random.roll(varianceIsFocusDependentProbability),
					clickCountMeanRange,
					clickCountVarianceRange,
					delayMeanRange,
					delayVarianceRange);
		}
	}

	private final Persona persona;
	private final ClientContext<?> ctx;
	private final boolean isDelayRandom;
	private final boolean isClickCountRandom;
	private final boolean isVarianceFocusDependent;
	private final Tuple2<Integer, Integer> clickCountMeanRange;
	@Nullable private final Tuple2<Double, Double> clickCountVarianceRange;
	private final Tuple2<Integer, Integer> delayMeanRange;
	@Nullable private final Tuple2<Double, Double> delayVarianceRange;
	private final int clickCountMean;
	private final int delayMean;
	private int interactProbability;

	private SpamClick(
			ClientContext<?> ctx,
			Persona persona,
			boolean isDelayRandom,
			boolean isClickCountRandom,
			boolean isVarianceFocusDependent,
			Tuple2<Integer, Integer> clickCountMeanRange,
			@Nullable Tuple2<Double, Double> clickCountVarianceRange,
			Tuple2<Integer, Integer> delayMeanRange,
			@Nullable Tuple2<Double, Double> delayVarianceRange) {
		this.ctx = ctx;
		this.persona = persona;
		this.isDelayRandom = isDelayRandom;
		this.isClickCountRandom = isClickCountRandom;
		this.isVarianceFocusDependent = isVarianceFocusDependent;
		this.clickCountMeanRange = clickCountMeanRange;
		this.clickCountVarianceRange = clickCountVarianceRange;
		this.delayMeanRange = delayMeanRange;
		this.delayVarianceRange = delayVarianceRange;
		this.clickCountMean = Random.nextInt(clickCountMeanRange);
		this.delayMean = Random.nextInt(delayMeanRange);
		newInteractProbability();
	}

	/**
	 * Returns the number of times to click on an interact event. This should be called and stored
	 * a single time when interacting.
	 * @return The number of times to click.
	 */
	public int getClicks() {
		newInteractProbability();
		return getConditionalRandomOrGauss(
				isClickCountRandom, clickCountMean, clickCountMeanRange, clickCountVarianceRange);
	}

	/**
	 * Returns the delay between each click on an interact event. This should be called every click
	 * event to determine delay.
	 * @return The delay, in milliseconds, between each click.
	 */
	public int getDelay() {
		return getConditionalRandomOrGauss(
				isDelayRandom, delayMean, delayMeanRange, delayVarianceRange);
	}

	/**
	 * Returns if the player should re-interact with whatever.
	 * @return If the player should re-interact with whatever.
	 */
	public boolean shouldReInteract() {
		return Random.roll(interactProbability);
	}

	/**
	 * Interacts using the given action using a randomly generated number of clicks that is
	 * dependent on the current user's persona.
<<<<<<< HEAD
	 *
	 * <p>Implementation note: This would've worked a lot cleaner for the caller if we could use
	 * lambda statements, but powerbot doesn't like Java 8.
	 * @param action
=======
	 * 
	 * <p> TODO: Add misclicking
	 * 
	 * <p>Implementation note: This would've worked a lot cleaner for the caller if we could use
	 * lambda statements, but powerbot doesn't like Java 8.
	 * @param action The single action to perform
>>>>>>> c071e1d8af3678d42105654706135e6a2328ab7a
	 */
	public void interact(Action action) {
		int clicks = getClicks();
		action.interact();
		while (clicks-- > 1) {
			if (shouldReInteract()) {
				action.interact();
			} else {
				ctx.input.click(true);
			}
		}
	}

	private int getConditionalRandomOrGauss(
			boolean isRandom,
			int mean,
			Tuple2<Integer, Integer> meanRange,
			Tuple2<Double, Double> varianceRange) {
		return isRandom
				? Random.nextInt(meanRange)
				: Random.nextSkewedGaussian(
						meanRange,
						mean,
						isVarianceFocusDependent
								? persona.getFocusScaledDouble(null, varianceRange)
								: Random.nextDouble(varianceRange));
	}

	private void newInteractProbability() {
		interactProbability = Random.nextInt(0, 100);
	}
}