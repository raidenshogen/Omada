package com.omada.realtime.event;

/**
 * Domain event fired by the card service after every card mutation
 * (create / update / delete). BoardSocketPublisher observes it and
 * forwards the change to the board's STOMP topic so other connected
 * clients stay in sync (Observer pattern via Spring ApplicationEvent).
 *
 * @param type           CARD_CREATED, CARD_UPDATED or CARD_DELETED
 * @param boardId        board the card belongs to
 * @param listId         list holding the card after the change
 * @param previousListId for moves: the list the card came from, else null
 * @param card           card state after the change (id-only for deletes)
 */
public record CardChangedEvent(
        String type,
        String boardId,
        String listId,
        String previousListId,
        Object card) {
}
