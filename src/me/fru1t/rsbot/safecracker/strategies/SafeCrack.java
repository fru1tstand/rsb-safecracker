package me.fru1t.rsbot.safecracker.strategies;

import java.util.concurrent.Callable;

import org.powerbot.script.Tile;
import org.powerbot.script.rt6.ClientContext;
import org.powerbot.script.rt6.GameObject;

import me.fru1t.common.annotations.Inject;
import me.fru1t.common.annotations.Singleton;
import me.fru1t.rsbot.RoguesDenSafeCracker;
import me.fru1t.rsbot.common.Timer;
import me.fru1t.rsbot.common.framework.Strategy;
import me.fru1t.rsbot.common.strategies.logic.SpamClick;
import me.fru1t.rsbot.common.util.Condition;
import me.fru1t.rsbot.safecracker.strategies.logic.Backpack;
import me.fru1t.rsbot.safecracker.strategies.logic.Health;
import me.fru1t.rsbot.safecracker.strategies.logic.InteractSpamClickProvider;
import me.fru1t.rsbot.safecracker.strategies.logic.SafeLogic;

/**
 * Defines the safe cracking portion of the script.
 *
 * <p>TODO: Add break points if the script get stopped before the entire method completes. Consider
 * splitting this method up into more states.
 *
 * <p>TODO: Add human behavior between actions and waiting.
 */
public class SafeCrack implements Strategy<RoguesDenSafeCracker.State> {
	private final ClientContext ctx;
	private final SpamClick spamClick;
	private final Health health;
	private final Backpack backpack;
	private final SafeLogic safeLogic;
	private final Timer safecrackAnimationTimer;

	private GameObject wallsafeGameObject;

	@Inject
	public SafeCrack(
			@Singleton ClientContext ctx,
			@Singleton InteractSpamClickProvider spamClickProvider,
			Health health,
			Backpack backpack,
			SafeLogic safeLogic,
			Timer safecrackAnimationTimer) {
		this.ctx = ctx;
		this.spamClick = spamClickProvider.get();
		this.health = health;
		this.backpack = backpack;
		this.safeLogic = safeLogic;
		this.safecrackAnimationTimer = safecrackAnimationTimer;

		wallsafeGameObject = ctx.objects.nil();
	}

	@Override
	public RoguesDenSafeCracker.State run() {
		// Bank run?
		// TODO: Add - Gamble (interact even when inventory is full)
		// TODO: Add - Eat food to open inventory space
		// Things to consider: More likely to gamble or eat to clear inventory when near a new
		// level?
		// Bank run?
		if (ctx.backpack.select().count() >= backpack.bankAt()) {
			backpack.newBankAt();
			// TODO: Move this somewhere more... appropriate.
			safeLogic.newSafe();
			return RoguesDenSafeCracker.State.BANK_WALK;
		}

		// Health low?
		if (ctx.combatBar.health() < health.eatAt()) {
			health.newEatAt();
			return RoguesDenSafeCracker.State.SAFE_EAT;
		}

		// Get safe
		if (!safeLogic.getSafe().location.equals(wallsafeGameObject.tile())) {
			wallsafeGameObject = ctx.objects.select()
					.at(safeLogic.getSafe().location)
					.id(RoguesDenSafeCracker.SAFE_OBJECT_ID).poll();
			wallsafeGameObject.bounds(RoguesDenSafeCracker.SAFE_OBJECT_BOUNDS_MODIFIER);

			if (wallsafeGameObject == ctx.objects.nil()) {
				return null;
			}
		}

		// TODO: Add other camera support
		if (!wallsafeGameObject.inViewport()) {
			ctx.camera.turnTo(wallsafeGameObject);
		}

		// Interact with safe
		spamClick.interact(new SpamClick.Action() {
			@Override
			public void interact() {
				wallsafeGameObject.click();
			}
		});

		// Waiting for the player to interact or fail
		if (!Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return ctx.players.local().animation()
						== RoguesDenSafeCracker.PLAYER_CRACK_ANIMATION
						|| !ctx.movement.destination().equals(Tile.NIL)
						|| !ctx.players.local().tile().equals(safeLogic.getSafe().playerLocation);
			}
		}, 100, 10)) { // 1000 ms
			return null;
		}

		// Safety check
		if (!ctx.movement.destination().equals(Tile.NIL)
				|| ctx.players.local().inMotion()
				|| !ctx.players.local().tile().equals(safeLogic.getSafe().playerLocation)) {
			return RoguesDenSafeCracker.State.SAFE_WALK; // Mistakes were made.
		}

		// Waiting for the player to success or fail
		if (!Condition.wait(
				new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return !wallsafeGameObject.valid()
								|| ctx.players.local().animation()
										== RoguesDenSafeCracker.PLAYER_CRACK_PRE_HURT_ANIMATION
								|| ctx.players.local().animation()
										== RoguesDenSafeCracker.PLAYER_HURTING_ANIMATION;
					}},
				new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return ctx.players.local().animation()
								== RoguesDenSafeCracker.PLAYER_CRACK_ANIMATION;
					}},
				safecrackAnimationTimer,
				2000,
				150)) {
			return null;
		}

		// Quick low health check before waiting
		if (ctx.combatBar.health() < health.eatAt()) {
			health.newEatAt();
			return RoguesDenSafeCracker.State.SAFE_EAT;
		}

		// Wait for safe reset
		if (!Condition.wait(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				return wallsafeGameObject.valid();
			}
		}, 300, 7)) { // 2100 ms
			return null;
		}

		return RoguesDenSafeCracker.State.SAFE_CRACK;
	}

}