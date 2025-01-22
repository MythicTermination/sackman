package de.cadentem.cave_dweller.entities.goals;

public enum Roll {
   CHASE(0),
   STARE(1),
   FLEE(2),
   STROLL(3);

   public final int rollValue;

   private Roll(int rollValue) {
      this.rollValue = rollValue;
   }

   public static Roll fromValue(int rollValue) {
      assert rollValue >= 0 && rollValue < values().length;

      return values()[rollValue];
   }

   // $FF: synthetic method
   private static Roll[] $values() {
      return new Roll[]{CHASE, STARE, FLEE, STROLL};
   }
}
