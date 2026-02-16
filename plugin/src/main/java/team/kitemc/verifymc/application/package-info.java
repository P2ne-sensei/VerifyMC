/**
 * Application layer orchestrates use cases.
 *
 * <p>Constraints:</p>
 * <ul>
 *   <li>Expose command/result models for adapters (web, command, etc.).</li>
 *   <li>Must not depend on {@code team.kitemc.verifymc.web} transport DTO or handler classes.</li>
 *   <li>Depend on abstractions and domain concepts instead of concrete persistence implementations.</li>
 * </ul>
 */
package team.kitemc.verifymc.application;
